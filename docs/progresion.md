# Dificultad progresiva

La temporada usa un único reloj para todos los mundos y jugadores. Cada día dura
24 horas de servidor encendido, incluso vacío. Los reinicios conservan el avance;
el tiempo apagado o con el módulo desactivado no cuenta. El día 30 es permanente.

## Activación

1. Copiar `progresion` del config.yml distribuido si la configuración existente
   todavía no contiene esta sección. No sustituir las otras secciones.
2. Establecer `progresion.enabled: true` y ejecutar `/ndh progresion recargar`.
3. Consultar `/ndh progresion calendario` y ejecutar `/ndh progresion iniciar`.

Instalar el plugin o habilitar el módulo no inicia una temporada automáticamente.
Una temporada guardada sí se reanuda al habilitarlo, conservando su pausa.

## Comandos y permisos

`/ndh progresion estado` muestra día, pausa y tiempo restante.
`/ndh progresion calendario [1-30]` muestra cambios diarios y, al indicar día,
las reglas acumuladas. Requieren `ndh.progresion.ver`, concedido por defecto.

`iniciar`, `pausar`, `continuar`, `dia <1-30>`, `reiniciar confirmar` y `recargar`
requieren `ndh.progresion.admin`, incluido en `ndh.admin`.
Pausar conserva las reglas activas. Cambiar de día conserva el estado de pausa;
reiniciar empieza el día 1 sin pausa. Retroceder no revierte daños, consumos ni
criaturas existentes. Desactivar el módulo detiene tanto el reloj como las reglas
por eventos; los atributos ya asignados a criaturas permanecen.

## Calendario y persistencia

`progresion.dias` debe contener los días 1 a 30. Los valores se heredan hasta que
otro día los sustituye: no se multiplican entre sí. Probabilidades y bonificaciones
usan fracciones de 0 a 1; pan-comida usa puntos enteros de 0 a 20 y pan-saturacion
usa puntos de 0 a 20. Una configuración inválida conserva el calendario activo.
El día 1 siempre establece Difícil.

Solo se modifican nuevos zombis, esqueletos y arañas normales de aparición NATURAL;
se excluyen variantes, spawners, huevos, comandos y refuerzos. No se añaden mobs.
La probabilidad de bebés sustituye el resultado vanilla mediante una sola tirada.
Los atributos usan bonificaciones respecto de su base, identificadas por el plugin.
Los proyectiles conservan la bonificación del esqueleto que los disparó.
ShareStats comparte la comida/saturación final sin volver a aplicar la penalización.

`plugins/ndhSurvival/progresion-state.yml` guarda reloj, pausa y dificultades
originales. Se reemplaza atómicamente cada minuto y al apagar. Un cierre brusco
puede perder aproximadamente el último minuto; un bloqueo del hilo del servidor
puede retrasar el guardado y los anuncios. No editar el estado con el servidor activo.
Ante corrupción o error de guardado, el módulo se bloquea y registra el error en
consola. Reparar o restaurar una copia válida antes de recargar. Un archivo ausente
se considera instalación nueva: no borrar este archivo para solucionar corrupción.

Al detenerse, restaura la dificultad original de mundos cargados solo si sigue
siendo Difícil. También restaura antes de descargar mundos y vuelve a imponer
Difícil al cargarlos con temporada activa. Comprueba las dificultades una vez por
segundo de ticks, sin recorrer entidades ni cargar chunks, para mantener Difícil
si otro comando la cambia durante una temporada activa.

## Verificación en servidor de pruebas

- Iniciar en Overworld y comprobar Difícil también en Nether y End; cargar un mundo
  después y comprobar la misma regla. Pausar, reiniciar el servidor y continuar.
- Usar `dia 2` y `dia 7`; consumir pan con hambre y saturación conocidas, con
  ShareStats activado y desactivado y con dos jugadores conectados.
- Comprobar apariciones naturales frente a spawners/huevos y que los mobs anteriores
  a un cambio de día mantengan sus atributos.
- Comprobar RTP, su protección de llegada y CreeperAsk; el módulo no modifica sus reglas.
- Usar `dia 30`, esperar, reiniciar y comprobar que no aparece un día 31.

Las pruebas automáticas cubren reloj, calendario, persistencia, cancelaciones y
permisos. Mockito se carga como agente de pruebas para Java 25, siguiendo su
[documentación oficial](https://github.com/mockito/mockito/blob/main/mockito-core/src/main/java/org/mockito/Mockito.java).
El balance requiere una prueba jugable antes de comenzar la temporada definitiva.
