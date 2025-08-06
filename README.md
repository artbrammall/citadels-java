# 🏰 Citadels — Java CLI Edition

A single-player, command-line version of the classic card game *Citadels*, written in Java with challenging and intelligent AI players, save/load functionality, modular architecture, and test coverage. Built using Gradle.

---

## 🎮 Gameplay Overview

Citadels is a strategic city-building game where players take on secret roles and use their unique abilities to gain gold, sabotage opponents, and build the most valuable city.

- Supports **4–7 players**
- Play against AI while taking the role of **Player 1**
- All Character powers like **Assassin**, **Thief**, **Architect**, **Magician** are fully implemented
- District powers (purple cards) implemented with extensible logic
- AI makes intelligent decisions using all of its available powers

> `![Original Game Reference](screenshots/original-citadels.jpg)`

---

## 🧱 Project Structure

```
citadels/
├── app/         # Main launcher & CLI logic (App.java)
├── core/        # Core game mechanics and loop
├── model/       # Cards, players, AI logic, interfaces
├── state/       # Board, decks, game state
├── test/        # JUnit test classes
└── resources/   # Card definitions (TSV), save files (JSON)
```

---

## 🚀 How to Run

### ✅ Prerequisites
- Java 8
- Gradle (or use the wrapper `./gradlew`)

### 🧩 Setup and Play

Clone and run:

```bash
git clone https://github.com/artbrammall/citadels-java.git
cd citadels-java
./gradlew jar
java -jar build/libs/citadels.jar
```

> 🎯 This will launch a fully playable text-based game in your terminal.

> `![Game Start](screenshots/game-start.png)`

---

## 🎯 Features

- Full **Character Selection Phase**
    - Hidden + visible discard logic
    - King logic (can’t be revealed)
- **Turn Phase Mechanics**
    - Choose income or cards
    - Build districts
    - Use character ability
    - Use purple district abilities
- **AI Logic**
    - AI chooses optimal character based on game state
    - Builds highest-value affordable districts
    - Makes intelligent special ability choices
- **Save/Load Game**
    - `save mygame.json`
    - `load mygame.json`
  > Save data stored in simple JSON format

- **JUnit Tests**
    - 50%+ test coverage
    - Core mechanics covered: scoring, deck setup, input handling

- **Modular Design**
    - Interfaces and polymorphism for AI/human players
    - Logic cleanly separated by package
    - Easy to extend with new characters, decks, or rules

> `![Gameplay](screenshots/midgame1.png)`

---

## ⌨️ Commands Summary

| Command          | Description                                                |
|------------------|------------------------------------------------------------|
| `t`              | Progress to next phase (turn/selection)                    |
| `hand`           | View hand and gold                                         |
| `build <n>`      | Build district in hand slot `n`                            |
| `citadel [p]`    | View player p's built districts                            |
| `action`         | Shows how to use your character's special action           |
| `info <name>`    | Info on a purple card or character                         |
| `all`            | Show summary of all players                                |
| `save <f>`       | Save game to `f.json`                                      |
| `load <f>`       | Load game from `f.json`                                    |
| `debug`          | Toggle AI hand visibility                                  |
| `end`            | End your turn                                              |

> `![Gameplay](screenshots/midgame2.png)`

---

## 🧠 Scoring System

- Base: Total cost of built districts
- Bonus: All 5 colours = +3 points
- First to 8 districts = +4 points
- Others who finish = +2 points
- Purple card bonuses applied
- Tie-break: Highest-ranked character wins

---

## 🧪 Running Tests

```bash
./gradlew test
```

Generate code coverage report:

```bash
./gradlew jacocoTestReport
```

HTML report generated in:  
`build/reports/jacoco/test/html/index.html`

> `![Coverage Report](screenshots/coverage.png)`

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).

---

## ✍️ Author

**Art Brammall**  
Undergraduate CS student at the University of Sydney  
📧 artbrammall@gmail.com  
🌐 https://github.com/artbrammall

---

## 💡 Interview Blurb / Resume Line

> *"Built a complete Java-based version of Citadels featuring intelligent AI logic, JSON save/load, modular design, and CLI gameplay. Developed with Gradle and IntelliJ, with extensive test coverage using JUnit."*

---

## 🏁 Future Additions

Open to expanding this project to include:

- GUI version (Swing or JavaFX)

Feedback welcome.
