queue4gae
=========

Queue4GAE is a task queue implementation for Google AppEngine for Java that replaces the built-in DeferredTask with a JSON-based implementation that still runs on TaskQueueService.


 * Tasks implemented with Queue4GAE are relying on TaskQueueService, the same as DeferredTasks. We are only changing the serialization mechanism from native to JSON.
 * Since tasks are serialized using JSON they can be inspected using the AppEngine console, meaning that you can always inspect the queue contents in case something goes wrong.
 * A single post URL, using the technology of your choice: Jersey, Spring MVC or HttpServlet if hardcore is your thing.
 * Specific extension points to @Inject fields into your tasks.
 * If your task does not have enough time, it will re-post itself again to continue where it left off.
 * Testing gets easier when you can mock your task execution environment.

Why native Java serialization sucks
---



<!--

JSON serialization. Now you can inspect the payload of your enqueued tasks
Deterministic behavior for tests. The asynchronous implementation of tasks in GAE makes it impossible to prevent when/how  [referencia al issue]
Automatic injection of field sin your tasks
Queue limit = 10 minutes. Query limit = 30 seconds

Example URi with servlets
Example with Guice
Example with SimpleDS

Testing your tasks: normal and delay
-->