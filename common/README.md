# Utilities and helpers for flow-pilot

## Params

Used to access parameters across the flow-pilot project.
### For python:

```python
from common.params import Params, DataType

params =  Params()
value = 2.3

# Writing values: These values will now be available across system.
paramss.put("key-float", value, DataType.FLOAT)
params.put("key-int", int(value), DataType.INT)

params.put("key-string", "string-value", DataType.STRING)
params.put("key-bytes", "string-value".encode(), DataType.BYTES)

# Getting values: data will have the correct type based on what was provided while putting.
data = params.get("key-any")

# In case a key dosen't exists, python version will return None.
data = params.get("imaginary-key") # None
```

### Java:

```java
import ai.flow.common.Params

ParamsInterface params = ParamsInterfac.getInstance();

// Putting values: use correspoding put functions for defferent types.
params.putInt("key-int", 56);
params.putFloat("key-float", 56.3f);

// Getting values"
// You should explicitly check if key is present in the db.
int data;
if (params.exists("key-int"))
	data = params.getInt("key-int");

// same for others
String data = params.getString("key-string");
byte[] data = params.getBytes("key-bytes");
```

## Path utlis:

Use `Path.internal("relative-path")` to get correct absolute path of flowpilot resources.

For example, to get correct absolute path for `core/ui` folder, use `Path.internal("core/ui")`.

## System Utils

Get system information like os, platform, architecture, etc.
