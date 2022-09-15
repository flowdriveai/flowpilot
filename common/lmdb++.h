/* This is free and unencumbered software released into the public domain. */

#ifndef LMDBXX_H
#define LMDBXX_H

/**
 * <lmdb++.h> - C++17 wrapper for LMDB.
 *
 * @author Arto Bendiken <arto@bendiken.net>
 * @maintainer Doug Hoyte <doug@hoytech.com>
 * @see https://github.com/hoytech/lmdbxx
 */

#ifndef __cplusplus
#error "<lmdb++.h> requires a C++ compiler"
#endif

#if __cplusplus < 201703L
#error "<lmdb++.h> requires a C++17 compiler (CXXFLAGS='-std=c++17')"
#endif

////////////////////////////////////////////////////////////////////////////////

#include <lmdb.h>      /* for MDB_*, mdb_*() */

#ifdef LMDBXX_DEBUG
#include <cassert>     /* for assert() */
#endif
#include <cstddef>     /* for std::size_t */
#include <cstdio>      /* for std::snprintf() */
#include <cstring>     /* for std::memcpy() */
#include <stdexcept>   /* for std::runtime_error */
#include <string>      /* for std::string */
#include <string_view> /* for std::string_view */
#include <limits>      /* for std::numeric_limits<> */
#include <memory>      /* for std::addressof */

namespace lmdb {
  using mode = mdb_mode_t;
}

////////////////////////////////////////////////////////////////////////////////
/* Error Handling */

namespace lmdb {
  class error;
  class logic_error;
  class fatal_error;
  class runtime_error;
  class key_exist_error;
  class not_found_error;
  class corrupted_error;
  class panic_error;
  class version_mismatch_error;
  class map_full_error;
  class bad_dbi_error;
}

/**
 * Base class for LMDB exception conditions.
 *
 * @see http://symas.com/mdb/doc/group__errors.html
 */
class lmdb::error : public std::runtime_error {
protected:
  const int _code;

public:
  /**
   * Throws an error based on the given LMDB return code.
   */
  [[noreturn]] static inline void raise(const char* origin, int rc);

  /**
   * Constructor.
   */
  error(const char* const origin,
        const int rc) noexcept
    : runtime_error{origin},
      _code{rc} {}

  /**
   * Returns the underlying LMDB error code.
   */
  int code() const noexcept {
    return _code;
  }

  /**
   * Returns the origin of the LMDB error.
   */
  const char* origin() const noexcept {
    return runtime_error::what();
  }

  /**
   * Returns the underlying LMDB error code.
   */
  virtual const char* what() const noexcept {
    static thread_local char buffer[1024];
    std::snprintf(buffer, sizeof(buffer),
      "%s: %s", origin(), ::mdb_strerror(code()));
    return buffer;
  }
};

/**
 * Base class for logic error conditions.
 */
class lmdb::logic_error : public lmdb::error {
public:
  using error::error;
};

/**
 * Base class for fatal error conditions.
 */
class lmdb::fatal_error : public lmdb::error {
public:
  using error::error;
};

/**
 * Base class for runtime error conditions.
 */
class lmdb::runtime_error : public lmdb::error {
public:
  using error::error;
};

/**
 * Exception class for `MDB_KEYEXIST` errors.
 *
 * @see http://symas.com/mdb/doc/group__errors.html#ga05dc5bbcc7da81a7345bd8676e8e0e3b
 */
class lmdb::key_exist_error final : public lmdb::runtime_error {
public:
  using runtime_error::runtime_error;
};

/**
 * Exception class for `MDB_NOTFOUND` errors.
 *
 * @see http://symas.com/mdb/doc/group__errors.html#gabeb52e4c4be21b329e31c4add1b71926
 */
class lmdb::not_found_error final : public lmdb::runtime_error {
public:
  using runtime_error::runtime_error;
};

/**
 * Exception class for `MDB_CORRUPTED` errors.
 *
 * @see http://symas.com/mdb/doc/group__errors.html#gaf8148bf1b85f58e264e57194bafb03ef
 */
class lmdb::corrupted_error final : public lmdb::fatal_error {
public:
  using fatal_error::fatal_error;
};

/**
 * Exception class for `MDB_PANIC` errors.
 *
 * @see http://symas.com/mdb/doc/group__errors.html#gae37b9aedcb3767faba3de8c1cf6d3473
 */
class lmdb::panic_error final : public lmdb::fatal_error {
public:
  using fatal_error::fatal_error;
};

/**
 * Exception class for `MDB_VERSION_MISMATCH` errors.
 *
 * @see http://symas.com/mdb/doc/group__errors.html#ga909b2db047fa90fb0d37a78f86a6f99b
 */
class lmdb::version_mismatch_error final : public lmdb::fatal_error {
public:
  using fatal_error::fatal_error;
};

/**
 * Exception class for `MDB_MAP_FULL` errors.
 *
 * @see http://symas.com/mdb/doc/group__errors.html#ga0a83370402a060c9175100d4bbfb9f25
 */
class lmdb::map_full_error final : public lmdb::runtime_error {
public:
  using runtime_error::runtime_error;
};

/**
 * Exception class for `MDB_BAD_DBI` errors.
 *
 * @since 0.9.14 (2014/09/20)
 * @see http://symas.com/mdb/doc/group__errors.html#gab4c82e050391b60a18a5df08d22a7083
 */
class lmdb::bad_dbi_error final : public lmdb::runtime_error {
public:
  using runtime_error::runtime_error;
};

inline void
lmdb::error::raise(const char* const origin,
                   const int rc) {
  switch (rc) {
    case MDB_KEYEXIST:         throw key_exist_error{origin, rc};
    case MDB_NOTFOUND:         throw not_found_error{origin, rc};
    case MDB_CORRUPTED:        throw corrupted_error{origin, rc};
    case MDB_PANIC:            throw panic_error{origin, rc};
    case MDB_VERSION_MISMATCH: throw version_mismatch_error{origin, rc};
    case MDB_MAP_FULL:         throw map_full_error{origin, rc};
#ifdef MDB_BAD_DBI
    case MDB_BAD_DBI:          throw bad_dbi_error{origin, rc};
#endif
    default:                   throw lmdb::runtime_error{origin, rc};
  }
}

////////////////////////////////////////////////////////////////////////////////
/* Procedural Interface: Metadata */

namespace lmdb {
  // TODO: mdb_version()
  // TODO: mdb_strerror()
}

////////////////////////////////////////////////////////////////////////////////
/* Procedural Interface: Environment */

namespace lmdb {
  static inline void env_create(MDB_env** env);
  static inline void env_open(MDB_env* env,
    const char* path, unsigned int flags, mode mode);
#if MDB_VERSION_FULL >= MDB_VERINT(0, 9, 14)
  static inline void env_copy(MDB_env* env, const char* path, unsigned int flags);
  static inline void env_copy_fd(MDB_env* env, mdb_filehandle_t fd, unsigned int flags);
#else
  static inline void env_copy(MDB_env* env, const char* path);
  static inline void env_copy_fd(MDB_env* env, mdb_filehandle_t fd);
#endif
  static inline void env_stat(MDB_env* env, MDB_stat* stat);
  static inline void env_info(MDB_env* env, MDB_envinfo* stat);
  static inline void env_sync(MDB_env* env, bool force);
  static inline void env_close(MDB_env* env) noexcept;
  static inline void env_set_flags(MDB_env* env, unsigned int flags, bool onoff);
  static inline void env_get_flags(MDB_env* env, unsigned int* flags);
  static inline void env_get_path(MDB_env* env, const char** path);
  static inline void env_get_fd(MDB_env* env, mdb_filehandle_t* fd);
  static inline void env_set_mapsize(MDB_env* env, std::size_t size);
  static inline void env_set_max_readers(MDB_env* env, unsigned int count);
  static inline void env_get_max_readers(MDB_env* env, unsigned int* count);
  static inline void env_set_max_dbs(MDB_env* env, MDB_dbi count);
  static inline unsigned int env_get_max_keysize(MDB_env* env);
#if MDB_VERSION_FULL >= MDB_VERINT(0, 9, 11)
  static inline void env_set_userctx(MDB_env* env, void* ctx);
  static inline void* env_get_userctx(MDB_env* env);
#endif
  // TODO: mdb_env_set_assert()
  // TODO: mdb_reader_list()
  static inline void reader_check(MDB_env *env, int *dead);
}

/**
 * @throws lmdb::error on failure
 * @see http://symas.com/mdb/doc/group__mdb.html#gaad6be3d8dcd4ea01f8df436f41d158d4
 */
static inline void
lmdb::env_create(MDB_env** env) {
  const int rc = ::mdb_env_create(env);
  if (rc != MDB_SUCCESS) {
    error::raise("mdb_env_create", rc);
  }
}

/**
 * @throws lmdb::error on failure
 * @see http://symas.com/mdb/doc/group__mdb.html#ga32a193c6bf4d7d5c5d579e71f22e9340
 */
static inline void
lmdb::env_open(MDB_env* const env,
               const char* const path,
               const unsigned int flags,
               const mode mode) {
  const int rc = ::mdb_env_open(env, path, flags, mode);
  if (rc != MDB_SUCCESS) {
    error::raise("mdb_env_open", rc);
  }
}

/**
 * @throws lmdb::error on failure
 * @see http://symas.com/mdb/doc/group__mdb.html#ga3bf50d7793b36aaddf6b481a44e24244
 * @see http://symas.com/mdb/doc/group__mdb.html#ga5d51d6130325f7353db0955dbedbc378
 */
static inline void
lmdb::env_copy(MDB_env* const env,
#if MDB_VERSION_FULL >= MDB_VERINT(0, 9, 14)
               const char* const path,
               const unsigned int flags = 0) {
  const int rc = ::mdb_env_copy2(env, path, flags);
#else
               const char* const path) {
  const int rc = ::mdb_env_copy(env, path);
#endif
  if (rc != MDB_SUCCESS) {
    error::raise("mdb_env_copy2", rc);
  }
}

/**
 * @throws lmdb::error on failure
 * @see http://symas.com/mdb/doc/group__mdb.html#ga5040d0de1f14000fa01fc0b522ff1f86
 * @see http://symas.com/mdb/doc/group__mdb.html#ga470b0bcc64ac417de5de5930f20b1a28
 */
static inline void
lmdb::env_copy_fd(MDB_env* const env,
#if MDB_VERSION_FULL >= MDB_VERINT(0, 9, 14)
                 const mdb_filehandle_t fd,
                 const unsigned int flags = 0) {
  const int rc = ::mdb_env_copyfd2(env, fd, flags);
#else
                 const mdb_filehandle_t fd) {
  const int rc = ::mdb_env_copyfd(env, fd);
#endif
  if (rc != MDB_SUCCESS) {
    error::raise("mdb_env_copyfd2", rc);
  }
}

/**
 * @throws lmdb::error on failure
 * @see http://symas.com/mdb/doc/group__mdb.html#gaf881dca452050efbd434cd16e4bae255
 */
static inline void
lmdb::env_stat(MDB_env* const env,
               MDB_stat* const stat) {
  const int rc = ::mdb_env_stat(env, stat);
  if (rc != MDB_SUCCESS) {
    error::raise("mdb_env_stat", rc);
  }
}

/**
 * @throws lmdb::error on failure
 * @see http://symas.com/mdb/doc/group__mdb.html#ga18769362c7e7d6cf91889a028a5c5947
 */
static inline void
lmdb::env_info(MDB_env* const env,
               MDB_envinfo* const stat) {
  const int rc = ::mdb_env_info(env, stat);
  if (rc != MDB_SUCCESS) {
    error::raise("mdb_env_info", rc);
  }
}

/**
 * @throws lmdb::error on failure
 * @see http://symas.com/mdb/doc/group__mdb.html#ga85e61f05aa68b520cc6c3b981dba5037
 */
static inline void
lmdb::env_sync(MDB_env* const env,
               const bool force = true) {
  const int rc = ::mdb_env_sync(env, force);
  if (rc != MDB_SUCCESS) {
    error::raise("mdb_env_sync", rc);
  }
}

/**
 * @see http://symas.com/mdb/doc/group__mdb.html#ga4366c43ada8874588b6a62fbda2d1e95
 */
static inline void
lmdb::env_close(MDB_env* const env) noexcept {
  ::mdb_env_close(env);
}

/**
 * @throws lmdb::error on failure
 * @see http://symas.com/mdb/doc/group__mdb.html#ga83f66cf02bfd42119451e9468dc58445
 */
static inline void
lmdb::env_set_flags(MDB_env* const env,
                    const unsigned int flags,
                    const bool onoff = true) {
  const int rc = ::mdb_env_set_flags(env, flags, onoff ? 1 : 0);
  if (rc != MDB_SUCCESS) {
    error::raise("mdb_env_set_flags", rc);
  }
}

/**
 * @throws lmdb::error on failure
 * @see http://symas.com/mdb/doc/group__mdb.html#ga2733aefc6f50beb49dd0c6eb19b067d9
 */
static inline void
lmdb::env_get_flags(MDB_env* const env,
                    unsigned int* const flags) {
  const int rc = ::mdb_env_get_flags(env, flags);
  if (rc != MDB_SUCCESS) {
    error::raise("mdb_env_get_flags", rc);
  }
}

/**
 * @throws lmdb::error on failure
 * @see http://symas.com/mdb/doc/group__mdb.html#gac699fdd8c4f8013577cb933fb6a757fe
 */
static inline void
lmdb::env_get_path(MDB_env* const env,
                   const char** path) {
  const int rc = ::mdb_env_get_path(env, path);
  if (rc != MDB_SUCCESS) {
    error::raise("mdb_env_get_path", rc);
  }
}

/**
 * @throws lmdb::error on failure
 * @see http://symas.com/mdb/doc/group__mdb.html#gaf1570e7c0e5a5d860fef1032cec7d5f2
 */
static inline void
lmdb::env_get_fd(MDB_env* const env,
                 mdb_filehandle_t* const fd) {
  const int rc = ::mdb_env_get_fd(env, fd);
  if (rc != MDB_SUCCESS) {
    error::raise("mdb_env_get_fd", rc);
  }
}

/**
 * @throws lmdb::error on failure
 * @see http://symas.com/mdb/doc/group__mdb.html#gaa2506ec8dab3d969b0e609cd82e619e5
 */
static inline void
lmdb::env_set_mapsize(MDB_env* const env,
                      const std::size_t size) {
  const int rc = ::mdb_env_set_mapsize(env, size);
  if (rc != MDB_SUCCESS) {
    error::raise("mdb_env_set_mapsize", rc);
  }
}

/**
 * @throws lmdb::error on failure
 * @see http://symas.com/mdb/doc/group__mdb.html#gae687966c24b790630be2a41573fe40e2
 */
static inline void
lmdb::env_set_max_readers(MDB_env* const env,
                          const unsigned int count) {
  const int rc = ::mdb_env_set_maxreaders(env, count);
  if (rc != MDB_SUCCESS) {
    error::raise("mdb_env_set_maxreaders", rc);
  }
}

/**
 * @throws lmdb::error on failure
 * @see http://symas.com/mdb/doc/group__mdb.html#ga70e143cf11760d869f754c9c9956e6cc
 */
static inline void
lmdb::env_get_max_readers(MDB_env* const env,
                          unsigned int* const count) {
  const int rc = ::mdb_env_get_maxreaders(env, count);
  if (rc != MDB_SUCCESS) {
    error::raise("mdb_env_get_maxreaders", rc);
  }
}

/**
 * @throws lmdb::error on failure
 * @see http://symas.com/mdb/doc/group__mdb.html#gaa2fc2f1f37cb1115e733b62cab2fcdbc
 */
static inline void
lmdb::env_set_max_dbs(MDB_env* const env,
                      const MDB_dbi count) {
  const int rc = ::mdb_env_set_maxdbs(env, count);
  if (rc != MDB_SUCCESS) {
    error::raise("mdb_env_set_maxdbs", rc);
  }
}

/**
 * @see http://symas.com/mdb/doc/group__mdb.html#gaaf0be004f33828bf2fb09d77eb3cef94
 */
static inline unsigned int
lmdb::env_get_max_keysize(MDB_env* const env) {
  const int rc = ::mdb_env_get_maxkeysize(env);
#ifdef LMDBXX_DEBUG
  assert(rc >= 0);
#endif
  return static_cast<unsigned int>(rc);
}

#if MDB_VERSION_FULL >= MDB_VERINT(0, 9, 11)
/**
 * @throws lmdb::error on failure
 * @since 0.9.11 (2014/01/15)
 * @see http://symas.com/mdb/doc/group__mdb.html#gaf2fe09eb9c96eeb915a76bf713eecc46
 */
static inline void
lmdb::env_set_userctx(MDB_env* const env,
                      void* const ctx) {
  const int rc = ::mdb_env_set_userctx(env, ctx);
  if (rc != MDB_SUCCESS) {
    error::raise("mdb_env_set_userctx", rc);
  }
}
#endif

#if MDB_VERSION_FULL >= MDB_VERINT(0, 9, 11)
/**
 * @since 0.9.11 (2014/01/15)
 * @see http://symas.com/mdb/doc/group__mdb.html#ga45df6a4fb150cda2316b5ae224ba52f1
 */
static inline void*
lmdb::env_get_userctx(MDB_env* const env) {
  return ::mdb_env_get_userctx(env);
}
#endif

static inline void
lmdb::reader_check(MDB_env *env, int *dead) {
  const int rc = ::mdb_reader_check(env, dead);
  if (rc != MDB_SUCCESS) {
    error::raise("reader_check", rc);
  }
}

////////////////////////////////////////////////////////////////////////////////
/* Procedural Interface: Transactions */

namespace lmdb {
  static inline void txn_begin(
    MDB_env* env, MDB_txn* parent, unsigned int flags, MDB_txn** txn);
  static inline MDB_env* txn_env(MDB_txn* txn) noexcept;
#ifdef LMDBXX_TXN_ID
  static inline std::size_t txn_id(MDB_txn* txn) noexcept;
#endif
  static inline void txn_commit(MDB_txn* txn);
  static inline void txn_abort(MDB_txn* txn) noexcept;
  static inline void txn_reset(MDB_txn* txn) noexcept;
  static inline void txn_renew(MDB_txn* txn);
}

/**
 * @throws lmdb::error on failure
 * @see http://symas.com/mdb/doc/group__mdb.html#gad7ea55da06b77513609efebd44b26920
 */
static inline void
lmdb::txn_begin(MDB_env* const env,
                MDB_txn* const parent,
                const unsigned int flags,
                MDB_txn** txn) {
  const int rc = ::mdb_txn_begin(env, parent, flags, txn);
  if (rc != MDB_SUCCESS) {
    error::raise("mdb_txn_begin", rc);
  }
}

/**
 * @see http://symas.com/mdb/doc/group__mdb.html#gaeb17735b8aaa2938a78a45cab85c06a0
 */
static inline MDB_env*
lmdb::txn_env(MDB_txn* const txn) noexcept {
  return ::mdb_txn_env(txn);
}

#ifdef LMDBXX_TXN_ID
/**
 * @note Only available in HEAD, not yet in any 0.9.x release (as of 0.9.16).
 */
static inline std::size_t
lmdb::txn_id(MDB_txn* const txn) noexcept {
  return ::mdb_txn_id(txn);
}
#endif

/**
 * @throws lmdb::error on failure
 * @see http://symas.com/mdb/doc/group__mdb.html#ga846fbd6f46105617ac9f4d76476f6597
 */
static inline void
lmdb::txn_commit(MDB_txn* const txn) {
  const int rc = ::mdb_txn_commit(txn);
  if (rc != MDB_SUCCESS) {
    error::raise("mdb_txn_commit", rc);
  }
}

/**
 * @see http://symas.com/mdb/doc/group__mdb.html#ga73a5938ae4c3239ee11efa07eb22b882
 */
static inline void
lmdb::txn_abort(MDB_txn* const txn) noexcept {
  ::mdb_txn_abort(txn);
}

/**
 * @see http://symas.com/mdb/doc/group__mdb.html#ga02b06706f8a66249769503c4e88c56cd
 */
static inline void
lmdb::txn_reset(MDB_txn* const txn) noexcept {
  ::mdb_txn_reset(txn);
}

/**
 * @throws lmdb::error on failure
 * @see http://symas.com/mdb/doc/group__mdb.html#ga6c6f917959517ede1c504cf7c720ce6d
 */
static inline void
lmdb::txn_renew(MDB_txn* const txn) {
  const int rc = ::mdb_txn_renew(txn);
  if (rc != MDB_SUCCESS) {
    error::raise("mdb_txn_renew", rc);
  }
}

////////////////////////////////////////////////////////////////////////////////
/* Procedural Interface: Databases */

namespace lmdb {
  static inline void dbi_open(
    MDB_txn* txn, const char* name, unsigned int flags, MDB_dbi* dbi);
  static inline void dbi_stat(MDB_txn* txn, MDB_dbi dbi, MDB_stat* stat);
  static inline void dbi_flags(MDB_txn* txn, MDB_dbi dbi, unsigned int* flags);
  static inline void dbi_close(MDB_env* env, MDB_dbi dbi) noexcept;
  static inline void dbi_drop(MDB_txn* txn, MDB_dbi dbi, bool del);
  static inline void dbi_set_compare(MDB_txn* txn, MDB_dbi dbi, MDB_cmp_func* cmp);
  static inline void dbi_set_dupsort(MDB_txn* txn, MDB_dbi dbi, MDB_cmp_func* cmp);
  static inline void dbi_set_relfunc(MDB_txn* txn, MDB_dbi dbi, MDB_rel_func* rel);
  static inline void dbi_set_relctx(MDB_txn* txn, MDB_dbi dbi, void* ctx);
  static inline bool dbi_get(MDB_txn* txn, MDB_dbi dbi, const MDB_val* key, MDB_val* data);
  static inline bool dbi_put(MDB_txn* txn, MDB_dbi dbi, const MDB_val* key, MDB_val* data, unsigned int flags);
  static inline bool dbi_del(MDB_txn* txn, MDB_dbi dbi, const MDB_val* key, const MDB_val* data);
  // TODO: mdb_cmp()
  // TODO: mdb_dcmp()
}

/**
 * @throws lmdb::error on failure
 * @see http://symas.com/mdb/doc/group__mdb.html#gac08cad5b096925642ca359a6d6f0562a
 */
static inline void
lmdb::dbi_open(MDB_txn* const txn,
               const char* const name,
               const unsigned int flags,
               MDB_dbi* const dbi) {
  const int rc = ::mdb_dbi_open(txn, name, flags, dbi);
  if (rc != MDB_SUCCESS) {
    error::raise("mdb_dbi_open", rc);
  }
}

/**
 * @throws lmdb::error on failure
 * @see http://symas.com/mdb/doc/group__mdb.html#gae6c1069febe94299769dbdd032fadef6
 */
static inline void
lmdb::dbi_stat(MDB_txn* const txn,
               const MDB_dbi dbi,
               MDB_stat* const result) {
  const int rc = ::mdb_stat(txn, dbi, result);
  if (rc != MDB_SUCCESS) {
    error::raise("mdb_stat", rc);
  }
}

/**
 * @throws lmdb::error on failure
 * @see http://symas.com/mdb/doc/group__mdb.html#ga95ba4cb721035478a8705e57b91ae4d4
 */
static inline void
lmdb::dbi_flags(MDB_txn* const txn,
                const MDB_dbi dbi,
                unsigned int* const flags) {
  const int rc = ::mdb_dbi_flags(txn, dbi, flags);
  if (rc != MDB_SUCCESS) {
    error::raise("mdb_dbi_flags", rc);
  }
}

/**
 * @see http://symas.com/mdb/doc/group__mdb.html#ga52dd98d0c542378370cd6b712ff961b5
 */
static inline void
lmdb::dbi_close(MDB_env* const env,
                const MDB_dbi dbi) noexcept {
  ::mdb_dbi_close(env, dbi);
}

/**
 * @see http://symas.com/mdb/doc/group__mdb.html#gab966fab3840fc54a6571dfb32b00f2db
 */
static inline void
lmdb::dbi_drop(MDB_txn* const txn,
               const MDB_dbi dbi,
               const bool del = false) {
  const int rc = ::mdb_drop(txn, dbi, del ? 1 : 0);
  if (rc != MDB_SUCCESS) {
    error::raise("mdb_drop", rc);
  }
}

/**
 * @throws lmdb::error on failure
 * @see http://symas.com/mdb/doc/group__mdb.html#ga68e47ffcf72eceec553c72b1784ee0fe
 */
static inline void
lmdb::dbi_set_compare(MDB_txn* const txn,
                      const MDB_dbi dbi,
                      MDB_cmp_func* const cmp = nullptr) {
  const int rc = ::mdb_set_compare(txn, dbi, cmp);
  if (rc != MDB_SUCCESS) {
    error::raise("mdb_set_compare", rc);
  }
}

/**
 * @throws lmdb::error on failure
 * @see http://symas.com/mdb/doc/group__mdb.html#gacef4ec3dab0bbd9bc978b73c19c879ae
 */
static inline void
lmdb::dbi_set_dupsort(MDB_txn* const txn,
                      const MDB_dbi dbi,
                      MDB_cmp_func* const cmp = nullptr) {
  const int rc = ::mdb_set_dupsort(txn, dbi, cmp);
  if (rc != MDB_SUCCESS) {
    error::raise("mdb_set_dupsort", rc);
  }
}

/**
 * @throws lmdb::error on failure
 * @see http://symas.com/mdb/doc/group__mdb.html#ga697d82c7afe79f142207ad5adcdebfeb
 */
static inline void
lmdb::dbi_set_relfunc(MDB_txn* const txn,
                      const MDB_dbi dbi,
                      MDB_rel_func* const rel) {
  const int rc = ::mdb_set_relfunc(txn, dbi, rel);
  if (rc != MDB_SUCCESS) {
    error::raise("mdb_set_relfunc", rc);
  }
}

/**
 * @throws lmdb::error on failure
 * @see http://symas.com/mdb/doc/group__mdb.html#ga7c34246308cee01724a1839a8f5cc594
 */
static inline void
lmdb::dbi_set_relctx(MDB_txn* const txn,
                     const MDB_dbi dbi,
                     void* const ctx) {
  const int rc = ::mdb_set_relctx(txn, dbi, ctx);
  if (rc != MDB_SUCCESS) {
    error::raise("mdb_set_relctx", rc);
  }
}

/**
 * @retval true  if the key/value pair was retrieved
 * @retval false if the key wasn't found
 * @see http://symas.com/mdb/doc/group__mdb.html#ga8bf10cd91d3f3a83a34d04ce6b07992d
 */
static inline bool
lmdb::dbi_get(MDB_txn* const txn,
              const MDB_dbi dbi,
              const MDB_val* const key,
              MDB_val* const data) {
  const int rc = ::mdb_get(txn, dbi, const_cast<MDB_val*>(key), data);
  if (rc != MDB_SUCCESS && rc != MDB_NOTFOUND) {
    error::raise("mdb_get", rc);
  }
  return (rc == MDB_SUCCESS);
}

/**
 * @retval true  if the key/value pair was inserted
 * @retval false if the key already existed
 * @see http://symas.com/mdb/doc/group__mdb.html#ga4fa8573d9236d54687c61827ebf8cac0
 */
static inline bool
lmdb::dbi_put(MDB_txn* const txn,
              const MDB_dbi dbi,
              const MDB_val* const key,
              MDB_val* const data,
              const unsigned int flags = 0) {
  const int rc = ::mdb_put(txn, dbi, const_cast<MDB_val*>(key), data, flags);
  if (rc != MDB_SUCCESS && rc != MDB_KEYEXIST) {
    error::raise("mdb_put", rc);
  }
  return (rc == MDB_SUCCESS);
}

/**
 * @retval true  if the key/value pair was removed
 * @retval false if the key wasn't found
 * @see http://symas.com/mdb/doc/group__mdb.html#gab8182f9360ea69ac0afd4a4eaab1ddb0
 */
static inline bool
lmdb::dbi_del(MDB_txn* const txn,
              const MDB_dbi dbi,
              const MDB_val* const key,
              const MDB_val* const data = nullptr) {
  const int rc = ::mdb_del(txn, dbi, const_cast<MDB_val*>(key), const_cast<MDB_val*>(data));
  if (rc != MDB_SUCCESS && rc != MDB_NOTFOUND) {
    error::raise("mdb_del", rc);
  }
  return (rc == MDB_SUCCESS);
}

////////////////////////////////////////////////////////////////////////////////
/* Procedural Interface: Cursors */

namespace lmdb {
  static inline void cursor_open(MDB_txn* txn, MDB_dbi dbi, MDB_cursor** cursor);
  static inline void cursor_close(MDB_cursor* cursor) noexcept;
  static inline void cursor_renew(MDB_txn* txn, MDB_cursor* cursor);
  static inline MDB_txn* cursor_txn(MDB_cursor* cursor) noexcept;
  static inline MDB_dbi cursor_dbi(MDB_cursor* cursor) noexcept;
  static inline bool cursor_get(MDB_cursor* cursor, MDB_val* key, MDB_val* data, MDB_cursor_op op);
  static inline bool cursor_put(MDB_cursor* cursor, MDB_val* key, MDB_val* data, unsigned int flags);
  static inline void cursor_del(MDB_cursor* cursor, unsigned int flags);
  static inline void cursor_count(MDB_cursor* cursor, std::size_t& count);
}

/**
 * @throws lmdb::error on failure
 * @see http://symas.com/mdb/doc/group__mdb.html#ga9ff5d7bd42557fd5ee235dc1d62613aa
 */
static inline void
lmdb::cursor_open(MDB_txn* const txn,
                  const MDB_dbi dbi,
                  MDB_cursor** const cursor) {
  const int rc = ::mdb_cursor_open(txn, dbi, cursor);
  if (rc != MDB_SUCCESS) {
    error::raise("mdb_cursor_open", rc);
  }
}

/**
 * @see http://symas.com/mdb/doc/group__mdb.html#gad685f5d73c052715c7bd859cc4c05188
 */
static inline void
lmdb::cursor_close(MDB_cursor* const cursor) noexcept {
  ::mdb_cursor_close(cursor);
}

/**
 * @throws lmdb::error on failure
 * @see http://symas.com/mdb/doc/group__mdb.html#gac8b57befb68793070c85ea813df481af
 */
static inline void
lmdb::cursor_renew(MDB_txn* const txn,
                   MDB_cursor* const cursor) {
  const int rc = ::mdb_cursor_renew(txn, cursor);
  if (rc != MDB_SUCCESS) {
    error::raise("mdb_cursor_renew", rc);
  }
}

/**
 * @see http://symas.com/mdb/doc/group__mdb.html#ga7bf0d458f7f36b5232fcb368ebda79e0
 */
static inline MDB_txn*
lmdb::cursor_txn(MDB_cursor* const cursor) noexcept {
  return ::mdb_cursor_txn(cursor);
}

/**
 * @see http://symas.com/mdb/doc/group__mdb.html#ga2f7092cf70ee816fb3d2c3267a732372
 */
static inline MDB_dbi
lmdb::cursor_dbi(MDB_cursor* const cursor) noexcept {
  return ::mdb_cursor_dbi(cursor);
}

/**
 * @throws lmdb::error on failure
 * @see http://symas.com/mdb/doc/group__mdb.html#ga48df35fb102536b32dfbb801a47b4cb0
 */
static inline bool
lmdb::cursor_get(MDB_cursor* const cursor,
                 MDB_val* const key,
                 MDB_val* const data,
                 const MDB_cursor_op op) {
  const int rc = ::mdb_cursor_get(cursor, key, data, op);
  if (rc != MDB_SUCCESS && rc != MDB_NOTFOUND) {
    error::raise("mdb_cursor_get", rc);
  }
  return (rc == MDB_SUCCESS);
}

/**
 * @throws lmdb::error on failure
 * @see http://symas.com/mdb/doc/group__mdb.html#ga1f83ccb40011837ff37cc32be01ad91e
 */
static inline bool
lmdb::cursor_put(MDB_cursor* const cursor,
                 MDB_val* const key,
                 MDB_val* const data,
                 const unsigned int flags = 0) {
  const int rc = ::mdb_cursor_put(cursor, key, data, flags);
  if (rc != MDB_SUCCESS && rc != MDB_KEYEXIST) {
    error::raise("mdb_cursor_put", rc);
  }
  return (rc == MDB_SUCCESS);
}

/**
 * @throws lmdb::error on failure
 * @see http://symas.com/mdb/doc/group__mdb.html#ga26a52d3efcfd72e5bf6bd6960bf75f95
 */
static inline void
lmdb::cursor_del(MDB_cursor* const cursor,
                 const unsigned int flags = 0) {
  const int rc = ::mdb_cursor_del(cursor, flags);
  if (rc != MDB_SUCCESS) {
    error::raise("mdb_cursor_del", rc);
  }
}

/**
 * @throws lmdb::error on failure
 * @see http://symas.com/mdb/doc/group__mdb.html#ga4041fd1e1862c6b7d5f10590b86ffbe2
 */
static inline void
lmdb::cursor_count(MDB_cursor* const cursor,
                   std::size_t& count) {
  const int rc = ::mdb_cursor_count(cursor, &count);
  if (rc != MDB_SUCCESS) {
    error::raise("mdb_cursor_count", rc);
  }
}

////////////////////////////////////////////////////////////////////////////////
/* Resource Interface: Environment */

namespace lmdb {
  class env;
}

/**
 * Resource class for `MDB_env*` handles.
 *
 * @note Instances of this class are movable, but not copyable.
 * @see http://symas.com/mdb/doc/group__internal.html#structMDB__env
 */
class lmdb::env {
protected:
  MDB_env* _handle{nullptr};

public:
  static constexpr unsigned int default_flags = 0;
  static constexpr mode default_mode = 0644; /* -rw-r--r-- */

  /**
   * Creates a new LMDB environment.
   *
   * @param flags
   * @throws lmdb::error on failure
   */
  static env create(const unsigned int flags = default_flags) {
    MDB_env* handle{nullptr};
    lmdb::env_create(&handle);
#ifdef LMDBXX_DEBUG
    assert(handle != nullptr);
#endif
    if (flags) {
      try {
        lmdb::env_set_flags(handle, flags);
      }
      catch (const lmdb::error&) {
        lmdb::env_close(handle);
        throw;
      }
    }
    return env{handle};
  }

  /**
   * Constructor.
   *
   * @param handle a valid `MDB_env*` handle
   */
  env(MDB_env* const handle) noexcept
    : _handle{handle} {}

  /**
   * Move constructor.
   */
  env(env&& other) noexcept {
    std::swap(_handle, other._handle);
  }

  /**
   * Move assignment operator.
   */
  env& operator=(env&& other) noexcept {
    if (this != &other) {
      std::swap(_handle, other._handle);
    }
    return *this;
  }

  /**
   * Destructor.
   */
  ~env() noexcept {
    try { close(); } catch (...) {}
  }

  /**
   * Returns the underlying `MDB_env*` handle.
   */
  operator MDB_env*() const noexcept {
    return _handle;
  }

  /**
   * Returns the underlying `MDB_env*` handle.
   */
  MDB_env* handle() const noexcept {
    return _handle;
  }

  /**
   * Flushes data buffers to disk.
   *
   * @param force
   * @throws lmdb::error on failure
   */
  void sync(const bool force = true) {
    lmdb::env_sync(handle(), force);
  }

  /**
   * Closes this environment, releasing the memory map.
   *
   * @note this method is idempotent
   * @post `handle() == nullptr`
   */
  void close() noexcept {
    if (handle()) {
      lmdb::env_close(handle());
      _handle = nullptr;
    }
  }

  int reader_check() {
    int dead;
    lmdb::reader_check(handle(), &dead);
    return dead;
  }

  /**
   * Opens this environment.
   *
   * @param path
   * @param flags
   * @param mode
   * @throws lmdb::error on failure
   */
  env& open(const char* const path,
            const unsigned int flags = default_flags,
            const mode mode = default_mode) {
    lmdb::env_open(handle(), path, flags, mode);
    return *this;
  }

  /**
   * @param flags
   * @param onoff
   * @throws lmdb::error on failure
   */
  env& set_flags(const unsigned int flags,
                 const bool onoff = true) {
    lmdb::env_set_flags(handle(), flags, onoff);
    return *this;
  }

  /**
   * @param size
   * @throws lmdb::error on failure
   */
  env& set_mapsize(const std::size_t size) {
    lmdb::env_set_mapsize(handle(), size);
    return *this;
  }

  /**
   * @param count
   * @throws lmdb::error on failure
   */
  env& set_max_readers(const unsigned int count) {
    lmdb::env_set_max_readers(handle(), count);
    return *this;
  }

  /**
   * @param count
   * @throws lmdb::error on failure
   */
  env& set_max_dbs(const MDB_dbi count) {
    lmdb::env_set_max_dbs(handle(), count);
    return *this;
  }

  /**
   * @notice WARNING: This is a function to access LMDB's internal memory map, use at your own risk!
   */
  std::string_view get_internal_map() {
    if (sizeof(int) != 4 || sizeof(long) != 8 || sizeof(void*) != 8) error::raise("get_internal_map: only LP64 supported", 0);

    // This is a hack that depends on the internal layout of LMDB's MDB_env struct. Hopefully there
    // will be a better way to get me_map at some point.

    char *me_map = *(char**)(((char*)_handle) + 56);

    MDB_envinfo arg;
    mdb_env_info(_handle, &arg);

    return std::string_view(me_map, arg.me_mapsize);
  }
};

////////////////////////////////////////////////////////////////////////////////
/* Resource Interface: Transactions */

namespace lmdb {
  class txn;
}

/**
 * Resource class for `MDB_txn*` handles.
 *
 * @note Instances of this class are movable, but not copyable.
 * @see http://symas.com/mdb/doc/group__internal.html#structMDB__txn
 */
class lmdb::txn {
protected:
  MDB_txn* _handle{nullptr};

public:
  static constexpr unsigned int default_flags = 0;

  /**
   * Creates a new LMDB transaction.
   *
   * @param env the environment handle
   * @param parent
   * @param flags
   * @throws lmdb::error on failure
   */
  static txn begin(MDB_env* const env,
                   MDB_txn* const parent = nullptr,
                   const unsigned int flags = default_flags) {
    MDB_txn* handle{nullptr};
    lmdb::txn_begin(env, parent, flags, &handle);
#ifdef LMDBXX_DEBUG
    assert(handle != nullptr);
#endif
    return txn{handle};
  }

  /**
   * Constructor.
   *
   * @param handle a valid `MDB_txn*` handle
   */
  txn(MDB_txn* const handle) noexcept
    : _handle{handle} {}

  /**
   * Move constructor.
   */
  txn(txn&& other) noexcept {
    std::swap(_handle, other._handle);
  }

  /**
   * Move assignment operator.
   */
  txn& operator=(txn&& other) noexcept {
    if (this != &other) {
      std::swap(_handle, other._handle);
    }
    return *this;
  }

  /**
   * Destructor.
   */
  ~txn() noexcept {
    if (_handle) {
      try { abort(); } catch (...) {}
      _handle = nullptr;
    }
  }

  /**
   * Returns the underlying `MDB_txn*` handle.
   */
  operator MDB_txn*() const noexcept {
    return _handle;
  }

  /**
   * Returns the underlying `MDB_txn*` handle.
   */
  MDB_txn* handle() const noexcept {
    return _handle;
  }

  /**
   * Returns the transaction's `MDB_env*` handle.
   */
  MDB_env* env() const noexcept {
    return lmdb::txn_env(handle());
  }

  /**
   * Commits this transaction.
   *
   * @throws lmdb::error on failure
   * @post `handle() == nullptr`
   */
  void commit() {
    auto h = _handle;
    _handle = nullptr;
    lmdb::txn_commit(h);
  }

  /**
   * Aborts this transaction.
   *
   * @post `handle() == nullptr`
   */
  void abort() noexcept {
    auto h = _handle;
    _handle = nullptr;
    lmdb::txn_abort(h);
  }

  /**
   * Resets this read-only transaction.
   */
  void reset() noexcept {
    lmdb::txn_reset(_handle);
  }

  /**
   * Renews this read-only transaction.
   *
   * @throws lmdb::error on failure
   */
  void renew() {
    lmdb::txn_renew(_handle);
  }
};

////////////////////////////////////////////////////////////////////////////////
/* Resource Interface: Databases */

namespace lmdb {
  class dbi;
}

/**
 * Resource class for `MDB_dbi` handles.
 *
 * @see http://symas.com/mdb/doc/group__mdb.html#gadbe68a06c448dfb62da16443d251a78b
 */
class lmdb::dbi {
protected:
  MDB_dbi _handle{(std::numeric_limits<MDB_dbi>::max)()};

public:
  static constexpr unsigned int default_flags     = 0;
  static constexpr unsigned int default_put_flags = 0;

  /**
   * Opens a database handle.
   *
   * @param txn the transaction handle
   * @param name the database name, or nullptr
   * @param flags dbi flags, ie MDB_CREATE
   * @throws lmdb::error on failure
   */
  static dbi
  open(MDB_txn* const txn,
       const char* const name = nullptr,
       const unsigned int flags = default_flags) {
    MDB_dbi handle{};
    lmdb::dbi_open(txn, name, flags, &handle);
    return dbi{handle};
  }

  /*
   * This overload is so that DBI names can be passed in as string_views, which can be
   * useful when storing the names of DBIs in the database itself. We need to convert it
   * to a std::string to ensure that it is NUL-byte terminated. This overload comes after
   * the const char* one above so that in that case there is no std::string overhead.
   */

  static dbi
  open(MDB_txn* const txn,
       std::string_view name,
       const unsigned int flags = default_flags) {
    MDB_dbi handle{};
    std::string nameStr(name);
    lmdb::dbi_open(txn, nameStr.c_str(), flags, &handle);
    return dbi{handle};
  }

  /**
   * Constructor.
   *
   * @note Creates an uninitialized dbi instance. You must move or move-assign a valid dbi instance onto it before you can use it.
   */
  dbi() noexcept
    : _handle{(std::numeric_limits<MDB_dbi>::max)()} {}

  /**
   * Constructor.
   *
   * @param handle a valid `MDB_dbi` handle
   */
  dbi(const MDB_dbi handle) noexcept
    : _handle{handle} {}

  /**
   * Destructor.
   */
  ~dbi() noexcept {
    if (_handle) {
      /* No need to call close() here. */
    }
  }

  /**
   * Returns the underlying `MDB_dbi` handle.
   */
  operator MDB_dbi() const noexcept {
    return _handle;
  }

  /**
   * Returns the underlying `MDB_dbi` handle.
   */
  MDB_dbi handle() const noexcept {
    return _handle;
  }

  /**
   * Returns statistics for this database.
   *
   * @param txn a transaction handle
   * @throws lmdb::error on failure
   */
  MDB_stat stat(MDB_txn* const txn) const {
    MDB_stat result;
    lmdb::dbi_stat(txn, handle(), &result);
    return result;
  }

  /**
   * Retrieves the flags for this database handle.
   *
   * @param txn a transaction handle
   * @throws lmdb::error on failure
   */
  unsigned int flags(MDB_txn* const txn) const {
    unsigned int result{};
    lmdb::dbi_flags(txn, handle(), &result);
    return result;
  }

  /**
   * Returns the number of records in this database.
   *
   * @param txn a transaction handle
   * @throws lmdb::error on failure
   */
  std::size_t size(MDB_txn* const txn) const {
    return stat(txn).ms_entries;
  }

  /**
   * @param txn a transaction handle
   * @param del
   * @throws lmdb::error on failure
   */
  void drop(MDB_txn* const txn,
            const bool del = false) {
    lmdb::dbi_drop(txn, handle(), del);
  }

  /**
   * Sets a custom key comparison function for this database.
   *
   * @param txn a transaction handle
   * @param cmp the comparison function
   * @throws lmdb::error on failure
   */
  dbi& set_compare(MDB_txn* const txn,
                   MDB_cmp_func* const cmp = nullptr) {
    lmdb::dbi_set_compare(txn, handle(), cmp);
    return *this;
  }

  /**
   * Retrieves a key/value pair from this database.
   *
   * @param txn a transaction handle
   * @param key
   * @param data
   * @throws lmdb::error on failure
   */

  bool get(MDB_txn* const txn,
           const std::string_view key,
           std::string_view& data) {
    const MDB_val keyV{key.size(), const_cast<char*>(key.data())};
    MDB_val dataV{data.size(), const_cast<char*>(data.data())};
    bool ret = lmdb::dbi_get(txn, handle(), &keyV, &dataV);
    if (ret) {
        data = std::string_view(static_cast<char*>(dataV.mv_data), dataV.mv_size);
    }
    return ret;
  }

  /**
   * Stores a key/value pair into this database.
   *
   * @param txn a transaction handle
   * @param key
   * @param data
   * @param flags
   * @throws lmdb::error on failure
   */
  bool put(MDB_txn* const txn,
           const std::string_view key,
           std::string_view data,
           const unsigned int flags = default_put_flags) {
    const MDB_val keyV{key.size(), const_cast<char*>(key.data())};
    MDB_val dataV{data.size(), const_cast<char*>(data.data())};
    return lmdb::dbi_put(txn, handle(), &keyV, &dataV, flags);
  }

  /**
   * Removes a key from this database.
   *
   * @param txn a transaction handle
   * @param key
   * @throws lmdb::error on failure
   */
  bool del(MDB_txn* const txn,
           const std::string_view key) {
    const MDB_val keyV{key.size(), const_cast<char*>(key.data())};
    return lmdb::dbi_del(txn, handle(), &keyV);
  }

  /**
   * Removes a key/value pair from this database.
   *
   * @param txn a transaction handle
   * @param key
   * @param val
   * @throws lmdb::error on failure
   */
  bool del(MDB_txn* const txn,
           const std::string_view key,
           const std::string_view val) {
    const MDB_val keyV{key.size(), const_cast<char*>(key.data())};
    const MDB_val valV{val.size(), const_cast<char*>(val.data())};
    return lmdb::dbi_del(txn, handle(), &keyV, &valV);
  }
};

////////////////////////////////////////////////////////////////////////////////
/* Resource Interface: Cursors */

namespace lmdb {
  class cursor;
}

/**
 * Resource class for `MDB_cursor*` handles.
 *
 * @note Instances of this class are movable, but not copyable.
 * @see http://symas.com/mdb/doc/group__internal.html#structMDB__cursor
 */
class lmdb::cursor {
protected:
  MDB_cursor* _handle{nullptr};

public:
  static constexpr unsigned int default_flags = 0;

  /**
   * Creates an LMDB cursor.
   *
   * @param txn the transaction handle
   * @param dbi the database handle
   * @throws lmdb::error on failure
   */
  static cursor
  open(MDB_txn* const txn,
       const MDB_dbi dbi) {
    MDB_cursor* handle{};
    lmdb::cursor_open(txn, dbi, &handle);
#ifdef LMDBXX_DEBUG
    assert(handle != nullptr);
#endif
    return cursor{handle};
  }

  /**
   * Constructor.
   *
   * @param handle a valid `MDB_cursor*` handle
   */
  cursor(MDB_cursor* const handle) noexcept
    : _handle{handle} {}

  /**
   * Move constructor.
   */
  cursor(cursor&& other) noexcept {
    std::swap(_handle, other._handle);
  }

  /**
   * Move assignment operator.
   */
  cursor& operator=(cursor&& other) noexcept {
    if (this != &other) {
      std::swap(_handle, other._handle);
    }
    return *this;
  }

  /**
   * Destructor.
   */
  ~cursor() noexcept {
    try { close(); } catch (...) {}
  }

  /**
   * Returns the underlying `MDB_cursor*` handle.
   */
  operator MDB_cursor*() const noexcept {
    return _handle;
  }

  /**
   * Returns the underlying `MDB_cursor*` handle.
   */
  MDB_cursor* handle() const noexcept {
    return _handle;
  }

  /**
   * Closes this cursor.
   *
   * @note this method is idempotent
   * @post `handle() == nullptr`
   */
  void close() noexcept {
    if (_handle) {
      lmdb::cursor_close(_handle);
      _handle = nullptr;
    }
  }

  /**
   * Renews this cursor.
   *
   * @param txn the transaction scope
   * @throws lmdb::error on failure
   */
  void renew(MDB_txn* const txn) {
    lmdb::cursor_renew(txn, handle());
  }

  /**
   * Returns the cursor's transaction handle.
   */
  MDB_txn* txn() const noexcept {
    return lmdb::cursor_txn(handle());
  }

  /**
   * Returns the cursor's database handle.
   */
  MDB_dbi dbi() const noexcept {
    return lmdb::cursor_dbi(handle());
  }

  /**
   * Retrieves a key from the database.
   *
   * @param key
   * @param op
   * @throws lmdb::error on failure
   */
  bool get(std::string_view &key,
           const MDB_cursor_op op) {
    MDB_val keyV{key.size(), const_cast<char*>(key.data())};
    bool ret = lmdb::cursor_get(handle(), &keyV, nullptr, op);
    if (ret) {
        key = std::string_view(static_cast<char*>(keyV.mv_data), keyV.mv_size);
    }
    return ret;
  }

  /**
   * Retrieves a key/value pair from the database.
   *
   * @param key
   * @param val
   * @param op
   * @throws lmdb::error on failure
   */
  bool get(std::string_view &key,
           std::string_view &val,
           const MDB_cursor_op op) {
    MDB_val keyV{key.size(), const_cast<char*>(key.data())};
    MDB_val valV{val.size(), const_cast<char*>(val.data())};
    bool ret = lmdb::cursor_get(handle(), &keyV, &valV, op);
    if (ret) {
        key = std::string_view(static_cast<char*>(keyV.mv_data), keyV.mv_size);
        val = std::string_view(static_cast<char*>(valV.mv_data), valV.mv_size);
    }
    return ret;
  }

  /**
   * Stores key/data pairs into the database. The cursor is positioned at the new item, or on failure usually near it.
   *
   * See MDB docs for flag values.
   *
   * @param key
   * @param val
   * @param flags
   * @throws lmdb::error on failure
   */
  bool put(const std::string_view &key,
           const std::string_view &val,
           const unsigned int flags = 0) {
    MDB_val keyV{key.size(), const_cast<char*>(key.data())};
    MDB_val valV{val.size(), const_cast<char*>(val.data())};
    return lmdb::cursor_put(handle(), &keyV, &valV, flags);
  }

  /**
   * Delete current key/data pair.
   *
   * @param flags Options for this operation. Values:
   *   MDB_NODUPDATA - delete all of the data items for the current key. This flag may only be specified if the database was opened with MDB_DUPSORT.
   */
  void del(unsigned int flags = 0) {
    lmdb::cursor_del(handle(), flags);
  }

  /**
   * Return count of duplicates for current key. This call is only valid on databases that support sorted duplicate data items MDB_DUPSORT.
   */
  size_t count() {
    std::size_t countp;
    lmdb::cursor_count(handle(), countp);
    return countp;
  }
};

namespace lmdb {
  /**
   * Creates a std::string_view that points to the memory pointed to by v.
   *
   * @param v
   */
  template<typename T>
  static inline std::string_view ptr_to_sv(T* v) {
    return std::string_view(reinterpret_cast<char*>(v), sizeof(*v));
  }

  /**
   * Creates a std::string_view that points to the memory occupied by v.
   *
   * @param v
   */
  template<typename T>
  static inline std::string_view to_sv(const T &v) {
    return std::string_view(reinterpret_cast<const char*>(std::addressof(v)), sizeof(v));
  }

  /**
   * Takes a std::string_view and casts its pointer as a pointer to the parameterized type.
   *
   * @param v
   */
  template<typename T>
  static inline T* ptr_from_sv(std::string_view v) {
    if (v.size() != sizeof(T)) error::raise("ptr_from_sv", MDB_BAD_VALSIZE);
    return reinterpret_cast<T*>(const_cast<char*>(v.data()));
  }

  /**
   * Takes a std::string_view and dereferences it, returning a value of the parameterized type.
   *
   * @param v
   */
  template<typename T>
  static inline T from_sv(std::string_view v) {
    if (v.size() != sizeof(T)) error::raise("from_sv", MDB_BAD_VALSIZE);
    T ret;
    std::memcpy(&ret, const_cast<char*>(v.data()), sizeof(T));
    return ret;
  }
}

////////////////////////////////////////////////////////////////////////////////

#endif /* LMDBXX_H */
