# queue4gae

Queue4GAE is a Java task queue wrapper for Google AppEngine that replaces the built-in DeferredTask serialization with a JSON-based implementation.

 * Tasks implemented with Queue4GAE use **the same Task Queue Service included in AppEngine**. They run just as DeferredTasks with a JSON serialization instead of native.
 * Since tasks are serialized using JSON **they can be inspected using the AppEngine console**, meaning that you can always inspect the queue contents in the AppEngine console in case something goes wrong. 
 * **A single post URL** using the technology of your choice: Jersey, Spring MVC or HttpServlet for the hardcore between us.
 * **A pluggable injection mechanism** to @Inject fields into your tasks.
 * In case of timeout, tasks will **automatically resume where they left off**.
 * **Easier, synchronous testing environment**.

## Getting started

The library can be downloaded from Maven:

```XML
<dependency>
   <groupId>org.extrema-sistemas</groupId>
   <artifactId>jackson4gae</artifactId>
</dependency>
<dependency>
   <groupId>org.extrema-sistemas</groupId>
   <artifactId>queue4gae</artifactId>
</dependency>
```

Queue4Gae can be configured manually or using any Dependency Injection framework. The following example uses Guice:

```Java
public class GuiceInjectionService implements InjectionService {

    private Injector injector;

    @Override
    public void injectMembers(Object instance) {
        injector.injectMembers(instance);
    }

    @Inject
    public void setInjector(Injector injector) {
        this.injector = injector;
    }

}
```

```Java
public class MyModule extends com.google.inject.AbstractModule {
  
  @Override
  protected void configure() {
    bindObjectMapper();
    bindQueue4Gae();
  }

  /**
   * An ObjectMapper instance that knows how to serialize GAE classes.
   * Put here the classes that will be attributes in your tasks
   */
  private void bindObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new GaeJacksonModule());
    bind(ObjectMapper.class).toInstance(objectMapper);
  }

  /** 
   * Configure Queue4Gae
   */
  private void bindQueue4Gae() {

    // Set the QueueService and InjectionService implementations
    bind(QueueService.class).to(QueueServiceImpl.class);
    bind(InjectionService.class).to(GuiceInjectionService.class);

    // Change this with your task URL
    bindConstant().annotatedWith(Names.named(QueueService.TASK_URL)).to("/task");
  }

}
```

Now you only need to register the URL that will receive serialized tasks at `/task` (or whatever URL you decide to use). This can be done using any web framework or even HttpServlet. Here is an example using JAX-RS:

```Java
public class Resource {

  @Inject
  private QueueService queueService;

  @POST @Path("/task")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response task(Task task) {
    queueService.run(task);
    return Response.noContent().build();
  }

}
```

### Writing your first task

Now you can write your first task. Tasks that extend `InjectedTask` will have their attributes injected before execution:

```Java
/**
 * Send a mail to a user. Queues will retry automatically if something fails.
 */
public class MailTask extends InjectedTask {

  // Important: add @JsonIgnore to skip from the JSON serialization
  @Inject @JsonIgnore
  private MyMailService mailService;

  /** the user key */
  private Key userKey;

  public MailTask() {
      // just for jackson
  }

  public MailTask(Key userKey) {
    super("my-mail-queue-name");
    this.userKey = userKey;
  }

  @Override
  public void run(QueueService queueService) {
      mailService.sendMessage(userKey);
  }

}
```

To use this task:

```Java
MailTask task = new MailTask(userKey);
queueService.post(task);
```

`InjectedTask` should be enough if your task processes a single entity, but for multiple results you should go with `CursorTask` instead.

## Queue limits and CursorTask

Tasks in AppEngine have a limit of 10 minutes to execute, but your queries are still limited to 30 seconds. CursorTask will handle this for you:

 * Subclasses must implement a `runQuery()` method that receive a Cursor instance and start processing. The method should periodically invoke queryTimeout() to check if it's close to exceed the 30-second limit, and in that case return the current Cursor value. As long as the value returned is not null (and if we are still below the 10-minute limit) the method will be invoked again with the provided Cursor to continue where it left off.
 * After 10 minutes, the task will be posted again to the same queue with the last known Cursor value returned from `runQuery()`.

`CursorTask` classes can use any persistence framework, as long as it supports native AppEngine Cursors. The following example uses <a href="https://github.com/icoloma/simpleds">SimpleDS</a>:

```Java
public class UpdateUserTask extends CursorTask {

  @Inject @JsonIgnore
  private EntityManager entityManager;

  @Inject @JsonIgnore
  private QueueService queueService;

  public UpdateTicketsEventTask() {
    super("default");
  }

  @Override
  protected Cursor runQuery(Cursor cursor) {
    CursorIterator<User> it = entityManager.createQuery(User.class)
        .withStartCursor(cursor)
        .asIterator();
    while (it.hasNext() && !queryTimeOut()) {
      User user = it.next();
      if (!user.isMailSent()) {
        queueService.post(new MailTask(user.getKey())); 
        user.setMailSent(true);
        entityManager.put(ticket);
      }
    }
    return it.hasNext()? it.getCursor() : null;
  }

}

```

This task will process all the users in the datastore and handle any timeouts transparently. Notice that the method checks if the mail has been already sent, since all AppEngine tasks must be idempotent in case the same task is executed multiple times.

## Task names

Just like AppEngine, in Queue4Gae you can specify a task name:

```Java
queueService.post(new MyTask().withTaskName("foobar"));
``` 

In this case the task name will be used the first time (and tombstoning applies as usual) but it will be cleared for subsequent executions. That is, if your task has to process one billion rows, the task name will only be applied to the first execution (that is, until we reach the 10-min timeout). After this timeout the tasl will be re-submitted again without any task name.

### Testing

Queue4Gae includes a mock implementation of QueueService intended for testing your tasks.

```Java
public class UpdateUserTaskTest {

  @Inject
  private MockQueueService queueService;

  @Inject 
  private EntityManager entityManager;

  @Test
  public void testTask() {
    // create sample data
    User user1 = MockUserFactory.create();
    User user2 = MockUserFactory.create();
    entityManager.put(ImmutableLIst.of(user1, user2));

    // execute the task
    queue.post(new UpdateUserTask());

    // 1 UpdateUserTask + 2 MailTasks = 3 Tasks
    assertEquals(3, queueService.getTaskCount());

    // check the data
    entityManager.refresh(user1);
    entityManager.refresh(user2);
    assertTrue(user1.isMailSent());
    assertTrue(user2.isMailSent());
  }

```

Tasks are executed synchronously and sequentially when using `MockQueueService`.

### Building queue4gae

Standard stuff. Clone the project, then execute one of the following:

```bash
# Generate the jar and upload to maven
gradle build && gradle upload

# Generate the IDEA project configuration
gradle idea
```
