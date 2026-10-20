# Hallazgos candidatos — Workshop Auth Backend

Prioridad evaluada para el laboratorio local documentado. Si el servicio se expone fuera de un entorno aislado, revisar la prioridad antes de desplegar. No se ejecutaron pruebas de validación.

### Token de autenticación sin firma
- Estado: CANDIDATO
- Evidencia: [LegacyTokenUtil.java](src/main/java/com/bcp/workshop/auth/LegacyTokenUtil.java#L9) contiene un secreto fijo y [LegacyTokenUtil.java](src/main/java/com/bcp/workshop/auth/LegacyTokenUtil.java#L13-L16) codifica username, rol, expiración y secreto en Base64. [AuthResource.java](src/main/java/com/bcp/workshop/auth/AuthResource.java#L39-L42) devuelve el valor como `token`.
- Impacto: los claims de identidad y rol no tienen integridad criptográfica; el token que consume la API puede representar identidad no confiable.
- Prioridad propuesta: P2 en el entorno local documentado; P1 si se expone.
- Contrato potencialmente afectado: respuesta de `POST /auth/login` y el token utilizado como Bearer por la API.
- Cómo validarlo: pruebas de integración deben confirmar el rechazo de tokens con claims alterados, firma inválida o expiración vencida.

### SQL de autenticación construido con datos interpolados
- Estado: CANDIDATO
- Evidencia: [LegacyDatabase.java](src/main/java/com/bcp/workshop/auth/LegacyDatabase.java#L32-L35) concatena usuario y contraseña en una consulta SQL. [LegacyDatabase.java](src/main/java/com/bcp/workshop/auth/LegacyDatabase.java#L22-L24) define la columna password y carga credenciales de laboratorio sin hash.
- Impacto: la entrada puede alterar la interpretación de la consulta de autenticación; además, las contraseñas guardadas como texto quedan expuestas ante acceso a la base.
- Prioridad propuesta: P2 en el entorno local documentado; P1 si se expone.
- Contrato potencialmente afectado: autenticación y respuesta de `POST /auth/login`.
- Cómo validarlo: probar en entorno aislado que credenciales con caracteres especiales se comparan como datos literales y verificar que el almacenamiento/comparación no requiere contraseñas en texto claro.

## Informe de discovery-analysis — 2026-10-20

### Respuesta de error expone detalles de excepciones
- Estado: CANDIDATO
- Evidencia: `src/main/java/com/bcp/workshop/auth/AuthResource.java:54-57` devuelve al cliente `error.getMessage()` y `error.getClass().getName()` en la respuesta HTTP.
- Impacto: errores internos podrían revelar detalles de implementación o persistencia a quien invoque el endpoint de login.
- Prioridad propuesta: P2 en el laboratorio local documentado; P1 si se expone fuera de un entorno aislado.
- Contrato potencialmente afectado: cuerpo de error de `POST /auth/login`, especialmente el campo `message` que el frontend muestra.
- Cómo validarlo: provocar una excepción controlada en una prueba de integración y comprobar que la respuesta externa sea genérica, conservando el detalle necesario en logs del servidor.
