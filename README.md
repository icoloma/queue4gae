# queue4gae

Queue4GAE is a Java task queue wrapper for Google AppEngine that replaces the built-in DeferredTask serialization with a JSON-based implementation.

 * Tasks implemented with Queue4GAE use **the same Task Queue Service included in AppEngine**. Think `DeferredTask` using JSON instead of native serialization.
 * Since they are using JSON, **serialized tasks can be inspected using the AppEngine console** when something goes wrong. 
 * Requires **a single URL** using any web technology: Jersey, Play, HttpServlet...
 * Includes **a pluggable injection mechanism** to `@Inject` fields into tasks.
 * In case of timeout, tasks will **automatically resume where they left off**.
 * Includes a **mock testing environment**.

## Getting started

Queue4Gae can be downloaded from Maven:

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

The configuration can be done using any Dependency Injection framework (or even by hand). An example using Guice:


```Java
public class MyModule extends com.google.inject.AbstractModule {
  
  @Override
  protected void configure() {
    bindObjectMapper();
    bindQueue4Gae();
  }

  // bind an ObjectMapper that knows how to handle classes specific to GAE
  private void bindObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new GaeJacksonModule());
    bind(ObjectMapper.class).toInstance(objectMapper);
  }

  // bind the classes required by Queue4Gae
  private void bindQueue4Gae() {

    bind(QueueService.class).to(QueueServiceImpl.class);
    bind(InjectionService.class).to(GuiceInjectionService.class);

    // Change this to your task URL
    bindConstant().annotatedWith(Names.named(QueueService.TASK_URL)).to("/task");
  }

}
```

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

The URL that will receive serialized tasks (in this example `/task`) can be implemented using any web technology. An example using JAX-RS:

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

Any task extending `InjectedTask` will have its attributes injected before execution:

```Java
/**
 * Send a mail to a user. Will be retried automatically if something fails.
 */
public class MailTask extends InjectedTask {

  // Important: add @JsonIgnore to skip from the JSON serialization
  @Inject @JsonIgnore
  private MyMailService mailService;

  private Key userKey;

  private MailTask() {
      // just for jackson deserialization
  }

  public MailTask(Key userKey) {
    super("default");
    this.userKey = userKey;
  }

  @Override
  public void run(QueueService queueService) {
      mailService.sendMessage(userKey);
  }

}

MailTask task = new MailTask(userKey);
queueService.post(task);
```

## Queue limits and CursorTask

Queue tasks will timeout after 10 minutes, and individual queries will timeout after 30 seconds. In order to work around these limitations, make the task extend `CursorTask`.

```Java
public class SendMailToUsersTask extends CursorTask {

  @Inject @JsonIgnore
  private EntityManager entityManager;

  @Inject @JsonIgnore
  private QueueService queueService;

  public UpdateTicketsEventTask() {
    super("default");
  }

  @Override
  protected Cursor runQuery(Cursor startCursor) {
    CursorIterator<User> it = entityManager.createQuery(User.class)
        .withStartCursor(startCursor)
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

Any persistence framework can be used as long as it supports native AppEngine Cursors. This example uses <a href="https://github.com/icoloma/simpleds">SimpleDS</a>. 

Subclasses of `CursorTask` must provide with a `runQuery()` method that will be invoked 
to process results starting with the provided cursor, if any. 
This method must check `queryTimeout()` at the beginning of each iteration to check that we are not close 
to the 30-second timeout, in which case it should exit and return the current Cursor. As long 
as the method returns a non-null Cursor (and if we are still below the 10-minute limit) 
`runQuery` will be invoked again to continue processing results.

After 10 minutes, the task will be re-submitted again to the queue with the last known `Cursor` value. 
This process will be repeated until `runQuery` returns null.

Notice that this example still checks if the mail has been already sent, since all task implementations must be idempotent.

## Task names

Tasks may specify a task name:

```Java
queueService.post(new MyTask().withTaskName("foobar"));
``` 

This task name will be used the first time (where tombstoning rules are applied) but it will be cleared for subsequent executions. For example, a task that must process one billion rows will apply the task name only to its first execution (and until the 10-min timeout is reached). After this execution the task name will be cleared before re-submitting the task again.

### Testing

Queue4Gae includes a mock implementation of QueueService for testing.

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
    queue.post(new SendMailToUsersTask());

    // 1 SendMailToUsersTask + 2 MailTasks = 3 Tasks
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
