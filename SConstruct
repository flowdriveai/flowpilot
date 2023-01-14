import os
import subprocess
import sysconfig
import numpy as np

arch = subprocess.check_output(["uname", "-m"], encoding='utf8').rstrip()

python_path = sysconfig.get_paths()['include']
cpppath = [
  "opendbc/can",
  '/usr/lib/include',
  python_path
]

AddOption('--kaitai',
          action='store_true',
          help='Regenerate kaitai struct parsers')
          
AddOption('--test',
          action='store_true',
          help='build test files')

AddOption('--asan',
          action='store_true',
          help='turn on ASAN')

AddOption('--ubsan',
          action='store_true',
          help='turn on UBSan')

AddOption('--clazy',
          action='store_true',
          help='build with clazy')

SHARED = False
real_arch = arch = subprocess.check_output(["uname", "-m"], encoding='utf8').rstrip()

lenv = {
  "PATH": os.environ['PATH'] + ":" + Dir(f"#libs/capnpc-java/{arch}/bin").abspath,
  "LD_LIBRARY_PATH": [Dir(f"#libs/acados/{arch}/lib").abspath],
  "PYTHONPATH": Dir("#").abspath + ":" + Dir("#pyextra/").abspath,

  "ACADOS_SOURCE_DIR": Dir("#libs/acados/include/acados").abspath,
  "ACADOS_PYTHON_INTERFACE_PATH": Dir("#pyextra/acados_template").abspath,
  "TERA_PATH": Dir("#").abspath + f"/libs/acados/{arch}/t_renderer",
}

libpath = [
      f"#libs/acados/{arch}/lib",
      f'#libs/mapbox-gl-native-qt/x86_64/{arch}'
      ]

cflags = []
cxxflags = []

rpath = lenv["LD_LIBRARY_PATH"].copy()
rpath += ["/usr/local/lib"]

rpath += [
    Dir("#cereal").abspath,
    Dir("#common").abspath
  ]

if GetOption('asan'):
  ccflags = ["-fsanitize=address", "-fno-omit-frame-pointer"]
  ldflags = ["-fsanitize=address"]
elif GetOption('ubsan'):
  ccflags = ["-fsanitize=undefined"]
  ldflags = ["-fsanitize=undefined"]
else:
  ccflags = []
  ldflags = []

cflags += ['-DSWAGLOG="\\"common/swaglog.h\\""']
cxxflags += ['-DSWAGLOG="\\"common/swaglog.h\\""']

env = Environment(
  ENV=lenv,
  CC='clang',
  CXX='clang++',
  CCFLAGS=[
    "-g",
    "-fPIC",
    "-O2",
    "-Wunused",
    "-Werror",
    "-Wshadow",
    "-Wno-unknown-warning-option",
    "-Wno-deprecated-register",
    "-Wno-register",
    "-Wno-inconsistent-missing-override",
    "-Wno-c99-designator",
    "-Wno-reorder-init-list",
    "-Wno-error=unused-but-set-variable",
  ] + cflags + ccflags,

  LINKFLAGS=ldflags,
  LIBPATH=libpath + [
    "#cereal",
    "#libs",
    "#opendbc/can/",
    "#libs/mapbox-gl-native-qt/x86_64",
    "#common",
    "#selfdrive/boardd",
    "#third_party",
  ],

  RPATH=rpath,

  CFLAGS=["-std=gnu11"],
  CXXFLAGS=["-std=c++1z"],
  CPPPATH=cpppath + [
    "#",
    "#libs/acados/include",
    "#libs/acados/include/blasfeo/include",
    "#libs/acados/include/hpipm/include",
    "#libs/mapbox-gl-native-qt/include",
    "#cereal",
    "#opendbc/can",
    "#common",
    "#third_party",
  ],
  CYTHONCFILESUFFIX=".cpp",
  toolpath = ['opendbc/site_scons/site_tools'],
  tools=["default", "cython"]
)

# Cython build enviroment
py_include = sysconfig.get_paths()['include']
envCython = env.Clone()
envCython["CPPPATH"] += [py_include, np.get_include()]
envCython["CCFLAGS"] += ["-Wno-#warnings", "-Wno-shadow", "-Wno-deprecated-declarations"]

envCython["LIBS"] = []
if arch == "Darwin":
  envCython["LINKFLAGS"] = ["-bundle", "-undefined", "dynamic_lookup"]
elif arch == "aarch64":
  envCython["LINKFLAGS"] = ["-shared"]
  envCython["LIBS"] = [os.path.basename(py_include)]
else:
  envCython["LINKFLAGS"] = ["-pthread", "-shared"]

Export('envCython')

# # Qt build environment
qt_env = env.Clone()
qt_modules = ["Widgets", "Gui", "Core", "Network", "Concurrent", "Multimedia", "Quick", "Qml", "QuickWidgets", "Location", "Positioning", "DBus"]

qt_libs = []
if arch == "Darwin":
  if real_arch == "arm64":
    qt_env['QTDIR'] = "/opt/homebrew/opt/qt@5"
  else:
    qt_env['QTDIR'] = "/usr/local/opt/qt@5"
  qt_dirs = [
    os.path.join(qt_env['QTDIR'], "include"),
  ]
  qt_dirs += [f"{qt_env['QTDIR']}/include/Qt{m}" for m in qt_modules]
  qt_env["LINKFLAGS"] += ["-F" + os.path.join(qt_env['QTDIR'], "lib")]
  qt_env["FRAMEWORKS"] += [f"Qt{m}" for m in qt_modules] + ["OpenGL"]
  qt_env.AppendENVPath('PATH', os.path.join(qt_env['QTDIR'], "bin"))
else:
  qt_install_prefix = subprocess.check_output(['qmake', '-query', 'QT_INSTALL_PREFIX'], encoding='utf8').strip()
  qt_install_headers = subprocess.check_output(['qmake', '-query', 'QT_INSTALL_HEADERS'], encoding='utf8').strip()

  qt_env['QTDIR'] = qt_install_prefix
  qt_dirs = [
    f"{qt_install_headers}",
    f"{qt_install_headers}/QtGui/5.12.8/QtGui",
  ]
  qt_dirs += [f"{qt_install_headers}/Qt{m}" for m in qt_modules]

  qt_libs = [f"Qt5{m}" for m in qt_modules]
  if arch == "larch64":
    qt_libs += ["GLESv2", "wayland-client"]
  elif arch != "Darwin":
    qt_libs += ["GL"]

qt_env.Tool('qt')
qt_env['CPPPATH'] += qt_dirs + ["#selfdrive/ui/qt/"]
qt_flags = [
  "-D_REENTRANT",
  "-DQT_NO_DEBUG",
  "-DQT_WIDGETS_LIB",
  "-DQT_GUI_LIB",
  "-DQT_QUICK_LIB",
  "-DQT_QUICKWIDGETS_LIB",
  "-DQT_QML_LIB",
  "-DQT_CORE_LIB",
  "-DQT_MESSAGELOGCONTEXT",
]
qt_flags
qt_env['CXXFLAGS'] += qt_flags
qt_env['LIBPATH'] += ['#selfdrive/ui']
qt_env['LIBS'] = qt_libs
# qt_env['QT_MOCHPREFIX'] = cache_dir + '/moc_files/moc_'

if GetOption("clazy"):
  checks = [
    "level0",
    "level1",
    "no-range-loop",
    "no-non-pod-global-static",
  ]
  qt_env['CXX'] = 'clazy'
  qt_env['ENV']['CLAZY_IGNORE_DIRS'] = qt_dirs[0]
  qt_env['ENV']['CLAZY_CHECKS'] = ','.join(checks)

Export('env', 'qt_env', 'arch', 'real_arch', 'SHARED')

QCOM_REPLAY = False
Export('env', 'arch', 'QCOM_REPLAY', 'SHARED')

SConscript(['common/SConscript'])
Import('_common')
if SHARED:
  common = abspath(common)
else:
  common = [_common, 'json11', 'lmdb']


Export('common')

envCython = env.Clone()
envCython["CPPPATH"] += [np.get_include()]
envCython["CCFLAGS"] += ["-Wno-#warnings", "-Wno-shadow", "-Wno-deprecated-declarations"]

python_libs = []
if arch == "Darwin":
  envCython["LINKFLAGS"] = ["-bundle", "-undefined", "dynamic_lookup"]
elif arch == "aarch64":
  envCython["LINKFLAGS"] = ["-shared"]

  python_libs.append(os.path.basename(python_path))
else:
  envCython["LINKFLAGS"] = ["-pthread", "-shared"]

envCython["LIBS"] = python_libs

Export('envCython')

# cereal and messaging are shared with the system
SConscript(['cereal/SConscript'])
if SHARED:
  cereal = abspath([File('cereal/libcereal_shared.so')])
  messaging = abspath([File('cereal/libmessaging_shared.so')])
else:
  cereal = [File('#cereal/libcereal.a')]
  messaging = [File('#cereal/libmessaging.a')]

Export('cereal', 'messaging')

# Build rednose library and ekf models

rednose_deps = [
  "#selfdrive/locationd/models/constants.py",
  "#selfdrive/locationd/models/gnss_helpers.py",
]

rednose_config = {
  'generated_folder': '#selfdrive/locationd/models/generated',
  'to_build': {
    'gnss': ('#selfdrive/locationd/models/gnss_kf.py', True, [], rednose_deps),
    'live': ('#selfdrive/locationd/models/live_kf.py', True, ['live_kf_constants.h'], rednose_deps),
    'car': ('#selfdrive/locationd/models/car_kf.py', True, [], rednose_deps),
  },
}

if arch != "larch64":
  rednose_config['to_build'].update({
    'loc_4': ('#selfdrive/locationd/models/loc_kf.py', True, [], rednose_deps),
    'pos_computer_4': ('#rednose/helpers/lst_sq_computer.py', False, [], []),
    'pos_computer_5': ('#rednose/helpers/lst_sq_computer.py', False, [], []),
    'feature_handler_5': ('#rednose/helpers/feature_handler.py', False, [], []),
    'lane': ('#xx/pipeline/lib/ekf/lane_kf.py', True, [], rednose_deps),
  })

Export('rednose_config')
SConscript(['rednose/SConscript'])

SConscript(['third_party/SConscript'])

SConscript(['SConscript'])
SConscript(['cereal/SConscript'])
SConscript(['panda/board/SConscript'])
SConscript(['opendbc/can/SConscript'])

SConscript(['common/kalman/SConscript'])
SConscript(['common/transformations/SConscript'])

SConscript(['selfdrive/controls/lib/cluster/SConscript'])
SConscript(['selfdrive/controls/lib/lateral_mpc_lib/SConscript'])
SConscript(['selfdrive/controls/lib/long_mpc_lib/SConscript'])

SConscript(['selfdrive/locationd/SConscript'])
SConscript(['selfdrive/boardd/SConscript'])
SConscript(['selfdrive/loggerd/SConscript'])
SConscript(['selfdrive/ui/SConscript'])
SConscript(['selfdrive/navd/SConscript'])

if GetOption('test'):
  SConscript('panda/tests/safety/SConscript')
