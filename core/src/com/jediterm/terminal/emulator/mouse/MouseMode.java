package com.jediterm.terminal.emulator.mouse;

/**
 * @author traff
 */
public enum MouseMode {
  MOUSE_REPORTING_NONE,
  // Taken from source code of https://github.com/xtermjs/xterm.js/blob/master/src/common/InputHandler.ts:1757
  MOUSE_REPORTING_NORMAL_ONLY_PRESS,
  MOUSE_REPORTING_NORMAL_PRESS_RELEASE,
  MOUSE_REPORTING_HILITE,
  MOUSE_REPORTING_BUTTON_MOTION,
  MOUSE_REPORTING_ALL_MOTION,
  MOUSE_REPORTING_FOCUS;
}
