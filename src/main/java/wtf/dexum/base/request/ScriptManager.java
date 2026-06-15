package wtf.dexum.base.request;

import com.darkmagician6.eventapi.EventManager;
import com.darkmagician6.eventapi.EventTarget;
import java.util.LinkedList;
import java.util.Queue;
import wtf.dexum.base.events.impl.player.EventRotate;
import wtf.dexum.base.events.impl.player.EventUpdate;

public class ScriptManager {
   private final Queue<ScriptManager.ScriptTask> tasks = new LinkedList();

   public ScriptManager() {
      EventManager.register(this);
   }

   public void tick(Object event) {
      ScriptManager.ScriptTask currentTask = (ScriptManager.ScriptTask)this.tasks.peek();
      if (currentTask != null) {
         boolean finished = currentTask.tryTick(event);
         if (finished) {
            this.tasks.poll();
         }

      }
   }

   @EventTarget
   public void rotateTick(EventRotate tickRotate) {
      this.tick(tickRotate);
   }

   @EventTarget(0)
   public void updateTick(EventUpdate tickUpdate) {
      this.tick(tickUpdate);
   }

   public void addTask(ScriptManager.ScriptTask task) {
      this.tasks.add(task);
   }

   public boolean isFinished() {
      return this.tasks.isEmpty();
   }

   public static class ScriptTask {
      private final Queue<ScriptManager.ScriptTask.Step<?>> steps = new LinkedList();
      private int idleTicks = 0;
      private int maxIdleTicks = 400;

      public ScriptManager.ScriptTask withMaxIdleTicks(int maxIdleTicks) {
         this.maxIdleTicks = Math.max(1, maxIdleTicks);
         return this;
      }

      public <E> ScriptManager.ScriptTask schedule(Class<E> eventClass, ScriptManager.ScriptTask.StepTask<E> action) {
         this.steps.add(new ScriptManager.ScriptTask.Step(eventClass, action));
         return this;
      }

      public boolean tryTick(Object event) {
         ScriptManager.ScriptTask.Step<?> nextStep = (ScriptManager.ScriptTask.Step)this.steps.peek();
         if (nextStep == null) {
            return true;
         } else {
            boolean progressed = false;
            if (nextStep.eventClass.isInstance(event)) {
               boolean stepDone = nextStep.execute(event);
               if (stepDone) {
                  this.steps.poll();
                  progressed = true;
               }
            }

            if (progressed) {
               this.idleTicks = 0;
               return this.steps.isEmpty();
            } else {
               ++this.idleTicks;
               if (this.idleTicks > this.maxIdleTicks) {
                  this.steps.clear();
                  return true;
               } else {
                  return false;
               }
            }
         }
      }

      public boolean isCompleted() {
         return this.steps.isEmpty();
      }

      private static class Step<E> {
         private final Class<E> eventClass;
         private final ScriptManager.ScriptTask.StepTask<E> action;

         Step(Class<E> eventClass, ScriptManager.ScriptTask.StepTask<E> action) {
            this.eventClass = eventClass;
            this.action = action;
         }

         boolean execute(Object event) {
            return this.action.accept(this.eventClass.cast(event));
         }
      }

      @FunctionalInterface
      public interface StepTask<E> {
         boolean accept(E var1);
      }
   }
}