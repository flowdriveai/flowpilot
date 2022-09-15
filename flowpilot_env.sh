ARCHNAME=$(arch)
SCRIPT=$(realpath "$0")
FLOWPILOT_DIR=$(dirname "$SCRIPT")

ENV_FILE=$FLOWPILOT_DIR/.env
rm -f $ENV_FILE

echo "export LD_LIBRARY_PATH=\$LD_LIBRARY_PATH:\"${FLOWPILOT_DIR}/libs/linux/${ARCHNAME}\"" >> $ENV_FILE
echo "export LD_LIBRARY_PATH=\$LD_LIBRARY_PATH:\"${FLOWPILOT_DIR}/libs/acados/${ARCHNAME}/lib\"" >> $ENV_FILE
echo "export PATH=\$PATH:\"${FLOWPILOT_DIR}/libs/capnpc-java/${ARCHNAME}/bin\"" >> $ENV_FILE
echo "export PYTHONPATH=\$PYTHONPATH:\"${FLOWPILOT_DIR}/pyextra\"" >> $ENV_FILE
echo "export ACADOS_SOURCE_DIR=\"${FLOWPILOT_DIR}/libs/acados/include/acados\"" >> $ENV_FILE
echo "export ACADOS_PYTHON_INTERFACE_PATH=\"${FLOWPILOT_DIR}/pyextra/acados_template\"" >> $ENV_FILE
echo "export TERA_PATH=\"${FLOWPILOT_DIR}/libs/acados/${ARCHNAME}/t_renderer\"" >> $ENV_FILE

