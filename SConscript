Import('env')

install_dir = "flowpilot.egg-info"
gen_setup_files = [
  f'{install_dir}/dependency_links.txt',
  f'{install_dir}/entry_points.txt',
  f'{install_dir}/PKG-INFO',
  f'{install_dir}/SOURCES.txt',
  f'{install_dir}/top_level.txt'
]

env.Command(['.env'], ['flowpilot_env.sh'], './flowpilot_env.sh')
setup = env.Command([gen_setup_files], ['setup.py'], 'pip install -e .')
#deps = env.Command(gen_setup_files, ['get_dependencies.sh'], './get_dependencies.sh')
#pip = env.Command(gen_setup_files, ['requirements.txt'], 'pip install -r requirements.txt')
