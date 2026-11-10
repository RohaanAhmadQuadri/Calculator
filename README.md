# Calculator

A modern dark-themed Android calculator with a scientific mode, editable expressions, calculation history, and a hidden math game. Built with **Java** and **XML layouts** (no Jetpack Compose).

> Built and tested on a Redmi 11 Prime.

---

## Features

### Calculator
- Basic operations: `+  −  ×  ÷  %`, decimals, backspace, and clear
- **Live result preview** while you type
- **Editable expression:** tap anywhere in the expression to move the cursor, then insert or delete at that spot
- **Comma formatting** for large numbers (`1,234,567`) in both the expression and the result
- Continue calculating from the previous result, or start fresh by typing a new number
- Clear error messages (for example, *Can't divide by 0*)

### Scientific mode
Tap the **Σ** icon (top right) to show or hide the scientific panel.

| Row | Buttons |
|-----|---------|
| 1 | `2nd`  `sin`  `cos`  `tan`  `π`  `e` |
| 2 | `ln`  `log`  `√`  `x²`  `xʸ`  `n!` |
| 3 | `(`  `)`  `1/x`  `x³`  `\|x\|`  `DEG/RAD` |

- **2nd** switches to inverse functions: `sin⁻¹`, `cos⁻¹`, `tan⁻¹`, `e^x`, `10^x`
- **DEG / RAD** toggle for trigonometric functions
- Brackets, powers, factorials, and percentages
- Implicit multiplication, so `2π` and `3(4+1)` work without typing `×`

### History
- Tap the **clock** icon to open a dark bottom-sheet history
- **Tap** an entry to reuse its result
- **Long-press** an entry to bring the expression back and edit it
- Saved on the device, so it survives closing the app (last 50 entries)

### Copy and paste
- **Long-press the result** to copy it (without commas)
- **Long-press the expression** to paste a number or expression from the clipboard (pasted text is cleaned so it can't break the calculator)

### Feel
- Press animation on every button
- Haptic (vibration) feedback on touch
- Animated result and scientific panel

### Hidden game: Math Blitz
**Long-press the history icon** to open it.

- 3 lives and a timer that shrinks as you level up
- Random question types that change every round: addition, subtraction, multiplication, division, squares, percentages, missing numbers, order of operations, square roots, and powers
- Answers are accepted as soon as they are correct
- Score, streak bonus, and a saved best score

---

## Screenshots

<!-- Add your screenshots to a `screenshots/` folder and update these paths -->
| Basic | Scientific | History | Math Blitz |
|:-----:|:----------:|:-------:|:----------:|
| ![Basic](screenshots/basic.png) | ![Scientific](screenshots/scientific.png) | ![History](screenshots/history.png) | ![Game](screenshots/game.png) |

---

## Getting started

### Requirements
- [Android Studio](https://developer.android.com/studio) (latest stable version)
- An Android phone with **USB debugging** turned on, or an emulator
- Minimum SDK: API 24 (Android 7.0), or whatever your project is set to

### Run the project
1. Clone the repository:
   ```bash
   git clone https://github.com/<your-username>/<your-repo-name>.git
   ```
2. Open the folder in Android Studio with **File > Open**.
3. Wait for Gradle sync to finish.
4. Select your device in the toolbar and click **Run**.

> **Tip:** keep the project outside cloud-synced folders such as OneDrive. Sync tools can lock build files and cause "Unable to delete directory" errors.

---

## Project structure

```
app/src/main/
├── java/com/example/calculator/
│   ├── MainActivity.java      # Calculator logic, parser, history, copy/paste
│   └── MathGame.java          # Math Blitz game dialog
└── res/
    ├── layout/
    │   └── activity_main.xml  # Calculator screen
    ├── drawable/
    │   ├── ic_sci.xml         # Scientific mode icon
    │   └── ic_history.xml     # History icon
    └── values/
        └── calc_styles.xml    # Button styles
```

---

## How it works

- **Expression editor:** the real expression is kept in a `StringBuilder` and drawn into an `EditText` with commas added. A position map converts between the displayed text and the real expression, so cursor taps land in the right place.
- **Parser:** a small recursive-descent parser handles operator precedence, brackets, unary minus, postfix operators (`%`, `!`, `²`, `³`), functions, and implicit multiplication. No external libraries are used.
- **History:** stored in `SharedPreferences`.
- **History sheet:** a `BottomSheetDialog` from Material Components, with the layout built in code.
- **Game:** built in code inside a full-screen `Dialog`, with a handler-based timer.

---

## Known limitations
- Portrait layout only (no dedicated landscape layout yet)
- The current expression is lost if the screen rotates
- Results of 1e15 or more are shown in scientific notation, and cannot be continued from

---

## Roadmap
- [ ] Light theme and theme toggle
- [ ] Memory buttons (M+, M−, MR, MC)
- [ ] Landscape layout with the scientific panel always visible
- [ ] Swipe to delete history entries
- [ ] Unit converter, EMI/loan, and GST calculators
- [ ] Settings screen (decimal places, default DEG/RAD)
- [ ] Custom app icon

---

## Contributing
Issues and pull requests are welcome. For larger changes, please open an issue first to discuss what you would like to change.

---

## License
Add a license of your choice here (for example, [MIT](https://choosealicense.com/licenses/mit/)).

---

## Author
**Your Name**: [@your-username](https://github.com/your-username)
