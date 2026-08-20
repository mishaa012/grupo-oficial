### Tabla: camillas

| Campo             | Tipo de dato | Descripción                           |
| ----------------- | ------------ | ------------------------------------- |
| id_camilla        | Integer      | Identificador principal de la camilla |
| codigo_camilla    | String       | Código único de la camilla            |
| tipo_camilla      | String       | Tipo de camilla                       |
| ubicacion_camilla | String       | ubicacion de camilla                  |
### Tabla: asignaciones_camilla

| Campo            | Tipo de dato | Descripción                       |
| ---------------- | ------------ | --------------------------------- |
| id_asignacion    | Integer      | Identificador principal           |
| id_camilla       | Integer      | Identificador de la camilla       |
| id_paciente      | Integer      | Identificador del paciente        |
| fecha_asignacion | Date         | Fecha de asignación               |
| hora_asignacion  |              | Hora de asignación                |
| fecha_salida     | Date         | Fecha en que se libera la camilla |
### Tabla: pacientes

| Campo               | Tipo de dato | Descripción                          |
| ------------------- | ------------ | ------------------------------------ |
| id_paciente         | Integer      | Identificador principal del paciente |
| nombre_paciente     | String       | Nombres del paciente                 |
| apellido_paterno    | String       | Apellido paterno                     |
| apellido_materno    | String       | Apellido materno                     |
| ci_paciente         | String       | Carnet de identidad                  |
| nacimiento_paciente | Date         | Fecha de nacimiento                  |
