package ai.flow.launcher;

import messaging.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class FlowInitd {
    ZMQ.Socket sock;
    ZContext ctx;

    public static Logger logger = LoggerFactory.getLogger(FlowInitd.class);

    // Signals
    public static final byte[] SIGSTART = "start_flag".getBytes();
    public static final byte[] SIGSTOP = "stop_flag".getBytes();

    public FlowInitd() {
        try {
            this.ctx = new ZContext();
            this.sock = this.ctx.createSocket(ZMQ.PUSH);

            // Connect to the flowinitd's IPC socket
            String uri = Utils.getSocketPath("6004");
            this.sock.connect(uri);
            logger.info("Connected to the FlowInit daemon");

        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    public void send(byte[] msg) {
        // Sends a message to the connected socket
        this.sock.send(msg, 0);
    }
}
