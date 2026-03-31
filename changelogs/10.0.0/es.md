## ¡Enlaces simbólicos! 🎉

**La versión 10.0.0** introduce los Enlaces simbólicos. Los Enlaces simbólicos te permiten tener el mismo rastreador, gráfico, función o incluso grupo en múltiples grupos. No es un duplicado ni una copia, es una referencia al mismo componente. Cualquier cambio que hagas en uno se reflejará en todos los demás. Para empezar con los Enlaces simbólicos, simplemente toca el botón + en la esquina superior derecha de cualquier grupo y selecciona "Enlace simbólico".

## ¡Nuevas acciones para puntos de datos! 📝

Ahora puedes añadir puntos de datos a un rastreador directamente desde la pantalla de historial (la que se abre al tocar la tarjeta del rastreador). Encontrarás el nuevo botón de acción flotante en la esquina inferior derecha de la pantalla.

![Acciones de puntos de datos](https://raw.githubusercontent.com/SamAmco/track-and-graph/refs/heads/master/changelogs/10.0.0/data_point_actions.jpg)

También hay un nuevo modo de selección múltiple en la pantalla de historial que te permite mover, copiar o eliminar varios puntos de datos a la vez. ¡Incluso puedes copiar puntos de datos de una función a un rastreador! Activa el modo de selección múltiple manteniendo pulsado un punto de datos, selecciona los que quieras y busca los nuevos botones de acción en la esquina inferior derecha de la pantalla.

## ¡Registro bloqueado! 🔒

Hay una nueva función en el diálogo de añadir punto de datos que te permite añadir varios puntos de datos para un rastreador de forma consecutiva. Busca el nuevo icono de candado al final de los campos de entrada:

![Registro bloqueado](https://raw.githubusercontent.com/SamAmco/track-and-graph/refs/heads/master/changelogs/10.0.0/locked_tracking.jpg)

Cuando se activa un candado, el diálogo permanecerá abierto después de añadir un punto de datos, y los campos bloqueados se rellenarán automáticamente con el mismo valor del punto de datos anterior.

## Correcciones de errores y mejoras

- Corregido: gráficos atascados en carga infinita (perdón por eso)
- Corregido: editar un recordatorio después de actualizarlo no abría el diálogo
- Corregido: traducciones faltantes para recordatorios
- Corregido: solicitudes únicas de work manager por recordatorio para evitar recordatorios duplicados
- Los recordatorios copiados ahora se programan inmediatamente
- Corregido: recordatorios copiados apareciendo en la ubicación incorrecta
- Corregido: notas no visibles debajo de los gráficos
- Corregido: la desviación estándar devuelve NaN por errores de precisión de punto flotante (en nodos de función)
- Corregido: índices de visualización no se actualizaban correctamente cuando los IDs coincidían
- Enlace del botón de información de scripts Lua a la guía del desarrollador en el diálogo de selección de nodos
- Mejora en la fiabilidad de los widgets de seguimiento tras actualizar la aplicación
- Actualización de dependencias de bibliotecas para mejorar el rendimiento y la estabilidad
- Ahora orientado a Android API nivel 36
            