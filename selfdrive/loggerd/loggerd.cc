#include "selfdrive/loggerd/loggerd.h"
#include <iostream>

ExitHandler do_exit;

struct LoggerdState {
  LoggerState logger = {};
  char segment_path[4096];
  std::atomic<int> rotate_segment;
  double last_rotate_tms = 0.;      // last rotate time in ms
};

void logger_rotate(LoggerdState *s) {
  int segment = -1;
  int err = logger_next(&s->logger, LOG_ROOT.c_str(), s->segment_path, sizeof(s->segment_path), &segment);
  assert(err == 0);
  s->rotate_segment = segment;
  s->last_rotate_tms = millis_since_boot();
  LOGW((s->logger.part == 0) ? "logging to %s" : "rotated to %s", s->segment_path);
}

void rotate_if_needed(LoggerdState *s) {
  double tms = millis_since_boot();
  if ((tms - s->last_rotate_tms) > SEGMENT_LENGTH * 1000 && !LOGGERD_TEST) {
    logger_rotate(s);
  }
}

void loggerd_thread() {
  // setup messaging
  typedef struct QlogState {
    std::string name;
    int counter, freq;
    } QlogState;
  std::unordered_map<SubSocket*, QlogState> qlog_states;

  std::unique_ptr<Context> ctx(Context::create());
  std::unique_ptr<Poller> poller(Poller::create());
  // subscribe to all socks
  for (const auto& it : services) {
    if (!it.should_log) continue;
    LOGD("logging %s (on port %d)", it.name, it.port);

    SubSocket * sock = SubSocket::create(ctx.get(), it.name);
    assert(sock != NULL);
    poller->registerSocket(sock);
    qlog_states[sock] = {
      .name = it.name,
      .counter = 0,
      .freq = it.decimation,
    };
  }

  LoggerdState s;
  // init logger
  logger_init(&s.logger, true);
  logger_rotate(&s);
  Params().put("CurrentRoute", s.logger.route_name);

  uint64_t msg_count = 0, bytes_count = 0;
  double start_ts = millis_since_boot();
  while (!do_exit) {
    // poll for new messages on all sockets
    for (auto sock : poller->poll(1000)) {
      if (do_exit) break;

      // drain socket
      int count = 0;
      QlogState &qs = qlog_states[sock];
      Message *msg = nullptr;
      while (!do_exit && (msg = sock->receive(true))) {
        const bool in_qlog = qs.freq != -1 && (qs.counter++ % qs.freq == 0);
  
        logger_log(&s.logger, (uint8_t *)msg->getData(), msg->getSize(), in_qlog);
        bytes_count += msg->getSize();
        delete msg;

        rotate_if_needed(&s);

        if ((++msg_count % 1000) == 0) {
          double seconds = (millis_since_boot() - start_ts) / 1000.0;
          LOGD("%lu messages, %.2f msg/sec, %.2f KB/sec", msg_count, msg_count / seconds, bytes_count * 0.001 / seconds);
        }

        count++;
        if (count >= 200) {
          LOGD("large volume of '%s' messages", qs.name.c_str());
          break;
        }
      }
    }
  }

  LOGW("closing logger");
  logger_close(&s.logger, &do_exit);

  if (do_exit.power_failure) {
    LOGE("power failure");
    sync();
    LOGE("sync done");
  }

  // messaging cleanup
  for (auto &[sock, qs] : qlog_states) delete sock;
}

int main(int argc, char** argv) {
  if (!Hardware::PC()) {
    int ret;
    ret = util::set_core_affinity({0, 1, 2, 3});
    assert(ret == 0);
  }

  loggerd_thread();

  return 0;
}
