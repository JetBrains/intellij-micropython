package com.jetbrains.micropython.repl

import com.jediterm.terminal.Terminal

// BACKCOMPAT: 2023.2: Inline it
fun Terminal.reset() = reset(true)
