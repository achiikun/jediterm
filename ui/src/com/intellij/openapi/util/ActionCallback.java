/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.openapi.util;

import com.intellij.util.Consumer;
import com.intellij.util.concurrency.Semaphore;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ActionCallback  {
  public static final ActionCallback DONE = new Done();
  public static final ActionCallback REJECTED = new Rejected();

  private final ExecutionCallback myDone;
  private final ExecutionCallback myRejected;

  protected String myError;

  private final String myName;

  public ActionCallback() {
    this(null);
  }

  public ActionCallback(String name) {
    myName = name;
    myDone = new ExecutionCallback();
    myRejected = new ExecutionCallback();
  }

  private ActionCallback(ExecutionCallback done, ExecutionCallback rejected) {
    myDone = done;
    myRejected = rejected;
    myName = null;
  }

  public ActionCallback(int countToDone) {
    this(null, countToDone);
  }

  public ActionCallback(String name, int countToDone) {
    myName = name;

    assert countToDone >= 0 : "count=" + countToDone;
    myDone = new ExecutionCallback(countToDone >= 1 ? countToDone : 1);
    myRejected = new ExecutionCallback();

    if (countToDone < 1) {
      setDone();
    }
  }

  public void setDone() {
    if (myDone.setExecuted()) {
      myRejected.clear();
    }
  }

  public boolean isDone() {
    return myDone.isExecuted();
  }

  public boolean isRejected() {
    return myRejected.isExecuted();
  }

  public boolean isProcessed() {
    return isDone() || isRejected();
  }

  public void setRejected() {
    if (myRejected.setExecuted()) {
      myDone.clear();
    }
  }

  @NotNull
  public ActionCallback reject(String error) {
    myError = error;
    setRejected();
    return this;
  }

  @Nullable
  public String getError() {
    return myError;
  }

  @NotNull
  public final ActionCallback doWhenDone(@NotNull final Runnable runnable) {
    myDone.doWhenExecuted(runnable);
    return this;
  }

  @NotNull
  public final ActionCallback doWhenRejected(@NotNull final Runnable runnable) {
    myRejected.doWhenExecuted(runnable);
    return this;
  }

  @NotNull
  public final ActionCallback doWhenRejected(@NotNull final Consumer<String> consumer) {
    myRejected.doWhenExecuted(new Runnable() {
      @Override
      public void run() {
        consumer.consume(myError);
      }
    });
    return this;
  }

  @NotNull
  public final ActionCallback doWhenProcessed(@NotNull final Runnable runnable) {
    doWhenDone(runnable);
    doWhenRejected(runnable);
    return this;
  }

  @NotNull
  public final ActionCallback notifyWhenDone(@NotNull final ActionCallback child) {
    return doWhenDone(child.createSetDoneRunnable());
  }

  @NotNull
  public final ActionCallback notifyWhenRejected(@NotNull final ActionCallback child) {
    return doWhenRejected(new Runnable() {
      @Override
      public void run() {
        child.reject(myError);
      }
    });
  }

  @NotNull
  public ActionCallback notify(@NotNull final ActionCallback child) {
    return doWhenDone(child.createSetDoneRunnable()).notifyWhenRejected(child);
  }

  @NotNull
  public final ActionCallback processOnDone(@NotNull Runnable runnable, boolean requiresDone) {
    if (requiresDone) {
      return doWhenDone(runnable);
    }
    runnable.run();
    return this;
  }

  public static class Done extends ActionCallback {
    public Done() {
      super(new ExecutedExecutionCallback(), new IgnoreExecutionCallback());
    }
  }

  public static class Rejected extends ActionCallback {
    public Rejected() {
      super(new IgnoreExecutionCallback(), new ExecutedExecutionCallback());
    }
  }

  private static class ExecutedExecutionCallback extends ExecutionCallback {
    public ExecutedExecutionCallback() {
      super(0);
    }

    @Override
    void doWhenExecuted(@NotNull Runnable runnable) {
      runnable.run();
    }

    @Override
    boolean setExecuted() {
      throw new IllegalStateException("Forbidden");
    }

    @SuppressWarnings("NonSynchronizedMethodOverridesSynchronizedMethod")
    @Override
    boolean isExecuted() {
      return true;
    }
  }

  private static class IgnoreExecutionCallback extends ExecutionCallback {
    @Override
    void doWhenExecuted(@NotNull Runnable runnable) {
    }

    @Override
    boolean setExecuted() {
      throw new IllegalStateException("Forbidden");
    }

    @SuppressWarnings("NonSynchronizedMethodOverridesSynchronizedMethod")
    @Override
    boolean isExecuted() {
      return false;
    }
  }

  @NonNls
  @Override
  public String toString() {
    final String name = myName != null ? myName : super.toString();
    return name + " done=[" + myDone + "] rejected=[" + myRejected + "]";
  }

  @NotNull
  public Runnable createSetDoneRunnable() {
    return new Runnable() {
      @Override
      public void run() {
        setDone();
      }
    };
  }

  /**
   * @deprecated use {@link #notifyWhenRejected(ActionCallback)}
   */
  @SuppressWarnings("UnusedDeclaration")
  @NotNull
  @Deprecated
  public Runnable createSetRejectedRunnable() {
    return new Runnable() {
      @Override
      public void run() {
        setRejected();
      }
    };
  }

  public boolean waitFor(long msTimeout) {
    if (isProcessed()) {
      return true;
    }

    final Semaphore semaphore = new Semaphore();
    semaphore.down();
    doWhenProcessed(new Runnable() {
      @Override
      public void run() {
        semaphore.up();
      }
    });

    try {
      if (msTimeout == -1) {
        semaphore.waitForUnsafe();
      }
      else if (!semaphore.waitForUnsafe(msTimeout)) {
        reject("Time limit exceeded");
        return false;
      }
    }
    catch (InterruptedException e) {
      reject(e.getMessage());
      return false;
    }
    return true;
  }
}