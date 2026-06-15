package wtf.dexum.base.request;

import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;
import net.minecraft.client.MinecraftClient;
import wtf.dexum.client.modules.api.Module;

public class RequestHandler<T> {
   private int currentTick = 0;
   private final PriorityBlockingQueue<RequestHandler.Request<T>> activeRequests = new PriorityBlockingQueue(11, Comparator.comparingInt((r) -> {
      return -((Request)r).priority;
   }));

   public void tick(int deltaTime) {
      this.currentTick += deltaTime;
   }

   public void tick() {
      this.tick(1);
   }

   public void request(RequestHandler.Request<T> request) {
      this.activeRequests.removeIf((existing) -> {
         return existing.provider == request.provider;
      });
      request.expiresIn += this.currentTick;
      this.activeRequests.add(request);
   }

   public T getActiveRequestValue() {
      RequestHandler.Request<T> top = (RequestHandler.Request)this.activeRequests.peek();
      if (top == null) {
         return null;
      } else {
         if (MinecraftClient.getInstance().isOnThread()) {
            while(top != null && (top.expiresIn <= this.currentTick || !top.provider.isEnabled())) {
               this.activeRequests.poll();
               top = (RequestHandler.Request)this.activeRequests.peek();
            }
         }

         return top != null ? top.value : null;
      }
   }

   public static class Request<T> {
      public int expiresIn;
      public final int priority;
      public final Module provider;
      public final T value;

      public Request(int expiresIn, int priority, Module provider, T value) {
         this.expiresIn = expiresIn;
         this.priority = priority;
         this.provider = provider;
         this.value = value;
      }
   }
}