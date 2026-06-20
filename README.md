# BetterAchievements

Een geavanceerde achievements plugin voor FrostNetwork.

## Functies
- 100+ configureerbare achievements.
- Zigzag GUI roadmap met pagina's.
- Sneakpeek voor vergrendelde achievements.
- Beloningen (Geld, Items, Commands).
- Actionbar progress updates.
- Modulaire configuratie.

## Installatie
1. Download de `BetterAchievements-1.0.0.jar`.
2. Plaats de jar in de `/plugins/` map van je server.
3. Start de server op.
4. Configureer de plugin in de `/plugins/BetterAchievements/` map.

## Commands
- `/achievements` - Open de achievements GUI.
- `/achievements reload` - Herlaad alle configuratiebestanden.
- `/betterachievements reset <speler>` - Reset de achievements van een speler.
- `/betterachievements resetall` - Reset alle achievement data, inclusief offline spelers.

## Permissies
- `betterachievements.admin` - Toegang tot het reload command.

## Build Instructies
Om de plugin zelf te compileren, gebruik Maven:
```bash
mvn clean package
```
De jar is dan te vinden in de `target/` map.
