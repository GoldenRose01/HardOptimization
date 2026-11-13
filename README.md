# HardOptimization

Mod **Fabric** per **Minecraft 26.3** (Fabric Loader `>=0.20.1`, Java `25`) focalizzata su prestazioni server (**20 TPS**, **MSPT < 30 ms**) e contenimento della memoria (**RAM < 16 GB**), mantenendo **100% parità vanilla** e **massima compatibilità con le altre mod**.

## Moduli inclusi (v0.1.0)

- **Tick Profiler & Adaptive Scheduler (`TickProfilerAndScheduler`)**:
  - Misurazione ad alta precisione (`System.nanoTime()`) di TPS, MSPT (`avg`, `min`, `p95`, `p99`, `max`), Heap RAM e pause GC.
  - Pool di worker CPU + **Java 25 Virtual Threads** per I/O asincrono, con svuotamento della coda sul thread principale limitato al budget MSPT configurato (`targetMsptBudget`, default `28.0 ms`).
- **Chunk Save Throttling (`ChunkSaveThrottling`)**:
  - Limita il lavoro di scaricamento/salvataggio chunk in `ChunkMap.tick` quando il tick corrente si avvicina al budget MSPT, evitando picchi di lag.
- **Shapes Fast-Path (`ShapesFastPath`)**:
  - Percorsi rapidi a zero allocazioni in `Shapes.joinIsNotEmpty` per confronti identici, vuoti (`EMPTY`) o pieni (`BLOCK`).
- **Entity Collision Fast-Path (`EntityCollisionFastPath`)**:
  - Uscita anticipata a zero allocazioni per bounding box degeneri in `EntityGetter.getEntityCollisions`.
- **Network Compression Tuning (`NetworkCompressionOptimization`)**:
  - Imposta il `Deflater` di `CompressionEncoder` al livello 4 (pienamente compatibile con tutti i client vanilla/moddati) riducendo il carico CPU della compressione pacchetti.
- **Compatibilità Automatica (`HardOptimizationMixinPlugin`)**:
  - Nessun `@Overwrite`. Se rileva mod come `lithium`, `c2me` o `krypton`, disattiva automaticamente i soli mixin sovrapposti.

## Comandi in-game / Console

Richiede permessi OP (`LEVEL_GAMEMASTERS`):
- `/hardopt` o `/hardopt status` — Mostra TPS, MSPT (avg/min/p95/p99/max), RAM usata/allocata/max, GC e coda scheduler.
- `/hardopt modules` — Stato di ogni modulo (`ACTIVE`, `DISABLED_BY_CONFIG`, `YIELDED_TO_MOD`).
- `/hardopt memory` — Dettaglio dei Memory Pool della JVM (`/hardopt memory gc` per GC diagnostico).
- `/hardopt reload` — Ricarica `config/hardoptimization.json`.

## Flag JVM consigliati (Java 25, Server < 16 GB RAM)

```bash
java -Xms8G -Xmx12G -XX:+UseZGC -XX:+AlwaysPreTouch -jar fabric-server-mc.26.3-loader.0.20.1.jar nogui
```

## Build

```bash
./gradlew build
```
Il JAR viene generato in `build/libs/hardoptimization-0.1.0.jar`.
