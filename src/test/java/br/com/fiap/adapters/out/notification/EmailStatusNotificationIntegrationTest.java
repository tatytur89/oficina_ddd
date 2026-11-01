package br.com.fiap.adapters.out.notification;

import br.com.fiap.domain.valueobjects.StatusOS;
import br.com.fiap.ports.out.StatusOsNotification;
import br.com.fiap.ports.out.StatusOsNotificationPort;
import jakarta.mail.Session;
import jakarta.mail.Part;
import jakarta.mail.Multipart;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/** A loopback-only SMTP sink captures mail in memory and never relays messages. */
@ExtendWith(OutputCaptureExtension.class)
class EmailStatusNotificationIntegrationTest {
    private static final StatusOsNotification NOTE = new StatusOsNotification(42L,StatusOS.EM_EXECUCAO,"cliente@test.invalid");

    @Test
    void smtpReceivesExactlyOneStatusMessageOnlyAfterTheDatabaseCommits() throws Exception {
        try(var fixture=new Fixture(false)){
            fixture.tx.executeWithoutResult(status->{
                fixture.jdbc.update("INSERT INTO notification_probe VALUES (1)");
                fixture.port.notificar(NOTE);
                assertEquals(0,fixture.smtp.connections.get());
                assertTrue(fixture.smtp.messages.isEmpty());
            });
            String message=fixture.smtp.messages.poll(2,TimeUnit.SECONDS);
            assertNotNull(message);
            assertTrue(fixture.smtp.messages.isEmpty());
            assertEquals(1,fixture.smtp.connections.get());
            assertEquals(1,fixture.smtp.loginAuthentications.get());
            assertEquals(1,fixture.jdbc.queryForObject("SELECT COUNT(*) FROM notification_probe",Integer.class));
            var parsed=new MimeMessage(Session.getInstance(new Properties()),new ByteArrayInputStream(message.getBytes(StandardCharsets.UTF_8)));
            assertEquals("oficina@test.invalid",parsed.getFrom()[0].toString());
            assertEquals("cliente@test.invalid",parsed.getAllRecipients()[0].toString());
            assertTrue(parsed.getSubject().contains("42"));
            assertTrue(parsed.isMimeType("multipart/*"));
            var plainParts=new ArrayList<String>();
            var htmlParts=new ArrayList<String>();
            collectBodyParts(parsed,plainParts,htmlParts);
            assertEquals(1,plainParts.size());
            assertEquals(1,htmlParts.size());
            for(String body:List.of(plainParts.getFirst(),htmlParts.getFirst())){
                assertTrue(body.contains("42"));
                assertTrue(body.contains(StatusOS.EM_EXECUCAO.getDescricao()));
                assertFalse(body.contains("chaveAcesso"));
                assertFalse(body.contains("JWT"));
                assertFalse(body.contains(NOTE.destinatario()));
                assertFalse(body.matches("(?is).*\\b(?:src|href|action)\\s*=\\s*['\"]?\\s*(?:https?:)?//.*"));
            }
            assertTrue(htmlParts.getFirst().toLowerCase().contains("<html"));
            assertTrue(htmlParts.getFirst().toLowerCase().contains("style"));
            assertNull(fixture.smtp.failure.get());
        }
    }

    private static void collectBodyParts(Part part,List<String> plainParts,List<String> htmlParts) throws Exception {
        if(part.isMimeType("multipart/*")){
            var multipart=(Multipart)part.getContent();
            for(int i=0;i<multipart.getCount();i++)collectBodyParts(multipart.getBodyPart(i),plainParts,htmlParts);
        }else if(part.isMimeType("text/plain")){
            assertTrue(part.getContentType().toLowerCase().contains("utf-8"));
            plainParts.add(part.getContent().toString());
        }else if(part.isMimeType("text/html")){
            assertTrue(part.getContentType().toLowerCase().contains("utf-8"));
            htmlParts.add(part.getContent().toString());
        }else{
            fail("Unexpected MIME body part: "+part.getContentType());
        }
    }

    @Test
    void rollbackSendsNoMailAndPersistsNoDatabaseChange() throws Exception {
        try(var fixture=new Fixture(false)){
            fixture.tx.executeWithoutResult(status->{
                fixture.jdbc.update("INSERT INTO notification_probe VALUES (1)");
                fixture.port.notificar(NOTE);
                status.setRollbackOnly();
            });
            assertEquals(0,fixture.jdbc.queryForObject("SELECT COUNT(*) FROM notification_probe",Integer.class));
            assertEquals(0,fixture.smtp.connections.get());
            assertTrue(fixture.smtp.messages.isEmpty());
        }
    }

    @Test
    void eventOutsideATransactionDoesNotSendMail() throws Exception {
        try(var fixture=new Fixture(false)){
            fixture.port.notificar(NOTE);
            assertEquals(0,fixture.smtp.connections.get());
            assertTrue(fixture.smtp.messages.isEmpty());
        }
    }

    @Test
    void smtpFailureCannotUndoCommittedDataOrExposeSmtpDetails(CapturedOutput output) throws Exception {
        try(var fixture=new Fixture(true)){
            assertDoesNotThrow(()->fixture.tx.executeWithoutResult(status->{
                fixture.jdbc.update("INSERT INTO notification_probe VALUES (1)");
                fixture.port.notificar(NOTE);
            }));
            assertEquals(1,fixture.jdbc.queryForObject("SELECT COUNT(*) FROM notification_probe",Integer.class));
            assertEquals(1,fixture.smtp.connections.get());
            assertTrue(fixture.smtp.messages.isEmpty());
            assertTrue(output.getAll().contains("status_os_email_failed osId=42 status=EM_EXECUCAO"));
            assertFalse(output.getAll().contains("sensitive-smtp-fixture-token"));
            assertFalse(output.getAll().contains("cliente@test.invalid"));
            assertNull(fixture.smtp.failure.get());
        }
    }

    @Configuration(proxyBeanMethods=false)
    @EnableTransactionManagement
    static class MailTestConfiguration {
        @Bean(destroyMethod="shutdown") EmbeddedDatabase dataSource(){
            return new EmbeddedDatabaseBuilder().generateUniqueName(true).setType(EmbeddedDatabaseType.H2).build();
        }
        @Bean PlatformTransactionManager transactionManager(DataSource dataSource){return new DataSourceTransactionManager(dataSource);}
        @Bean TransactionTemplate transactionTemplate(PlatformTransactionManager manager){return new TransactionTemplate(manager);}
        @Bean JavaMailSender sender(SmtpSink sink){
            var sender=new JavaMailSenderImpl();
            sender.setHost("127.0.0.1");
            sender.setPort(sink.server.getLocalPort());
            sender.setProtocol("smtp");
            sender.setDefaultEncoding("UTF-8");
            sender.setUsername("test-sender");
            sender.setPassword("fake-smtp-password");
            var properties=new Properties();
            properties.setProperty("mail.smtp.auth","true");
            properties.setProperty("mail.smtp.auth.mechanisms","LOGIN");
            properties.setProperty("mail.smtp.ssl.enable","false");
            properties.setProperty("mail.smtp.starttls.enable","false");
            properties.setProperty("mail.smtp.connectiontimeout","1000");
            properties.setProperty("mail.smtp.timeout","1000");
            properties.setProperty("mail.smtp.writetimeout","1000");
            sender.setJavaMailProperties(properties);
            return sender;
        }
        @Bean StatusOsNotificationPort port(ApplicationEventPublisher publisher){return new TransactionalStatusOsNotificationPublisher(publisher);}
        @Bean SmtpStatusOsNotificationListener listener(JavaMailSender sender){return new SmtpStatusOsNotificationListener(sender,"oficina@test.invalid");}
    }

    static class Fixture implements AutoCloseable {
        final SmtpSink smtp;
        final AnnotationConfigApplicationContext context;
        final JdbcTemplate jdbc;
        final TransactionTemplate tx;
        final StatusOsNotificationPort port;
        Fixture(boolean reject) throws IOException {
            smtp=new SmtpSink(reject);
            context=new AnnotationConfigApplicationContext();
            context.registerBean(SmtpSink.class,()->smtp);
            context.register(MailTestConfiguration.class);
            context.refresh();
            jdbc=new JdbcTemplate(context.getBean(DataSource.class));
            jdbc.execute("CREATE TABLE notification_probe(id INTEGER PRIMARY KEY)");
            tx=context.getBean(TransactionTemplate.class);
            port=context.getBean(StatusOsNotificationPort.class);
        }
        @Override public void close() throws Exception {try{context.close();}finally{smtp.close();}}
    }

    static class SmtpSink implements AutoCloseable {
        final ServerSocket server;
        final BlockingQueue<String> messages=new LinkedBlockingQueue<>();
        final AtomicInteger connections=new AtomicInteger();
        final AtomicInteger loginAuthentications=new AtomicInteger();
        final AtomicReference<Throwable> failure=new AtomicReference<>();
        final AtomicBoolean closed=new AtomicBoolean();
        final boolean reject;
        final ExecutorService worker=Executors.newSingleThreadExecutor(task->{var thread=new Thread(task,"loopback-smtp-test");thread.setDaemon(true);return thread;});
        SmtpSink(boolean reject) throws IOException {
            this.reject=reject;
            server=new ServerSocket();
            server.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"),0));
            server.setSoTimeout(1000);
            worker.submit(this::serve);
        }
        void serve(){
            while(!closed.get()){
                try(var socket=server.accept()){
                    connections.incrementAndGet();
                    socket.setSoTimeout(3000);
                    handle(socket);
                }catch(SocketTimeoutException ignored){
                }catch(Exception exception){if(!closed.get())failure.compareAndSet(null,exception);}
            }
        }
        void handle(Socket socket) throws IOException {
            var reader=new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.UTF_8));
            var writer=new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(),StandardCharsets.UTF_8));
            reply(writer,"220 localhost ESMTP test-only");
            String command;
            while((command=reader.readLine())!=null){
                if(command.startsWith("EHLO")||command.startsWith("HELO")){reply(writer,"250-localhost");reply(writer,"250-AUTH LOGIN");reply(writer,"250 SIZE 1048576");}
                else if(command.startsWith("AUTH LOGIN")){
                    String[] parts=command.split(" ");
                    String username;
                    if(parts.length>2){username=parts[2];}else{reply(writer,"334 VXNlcm5hbWU6");username=reader.readLine();}
                    reply(writer,"334 UGFzc3dvcmQ6");
                    String password=reader.readLine();
                    var decoder=java.util.Base64.getDecoder();
                    if("test-sender".equals(new String(decoder.decode(username),StandardCharsets.UTF_8))&&
                            "fake-smtp-password".equals(new String(decoder.decode(password),StandardCharsets.UTF_8))){
                        loginAuthentications.incrementAndGet();reply(writer,"235 Authentication successful");
                    }else{reply(writer,"535 Authentication rejected");}
                }
                else if(command.startsWith("MAIL FROM")){reply(writer,reject?"550 sensitive-smtp-fixture-token":"250 OK");}
                else if(command.startsWith("RCPT TO")){reply(writer,"250 OK");}
                else if(command.equals("DATA")){
                    reply(writer,"354 End data with a dot");
                    var data=new StringBuilder();
                    String line;
                    while((line=reader.readLine())!=null&&!line.equals(".")){data.append(line.startsWith("..")?line.substring(1):line).append("\r\n");}
                    messages.add(data.toString());
                    reply(writer,"250 Queued in memory only");
                }else if(command.equals("QUIT")){reply(writer,"221 Bye");return;}
                else if(command.equals("RSET")||command.equals("NOOP")){reply(writer,"250 OK");}
                else{reply(writer,"502 Unsupported in loopback sink");}
            }
        }
        void reply(BufferedWriter writer,String response) throws IOException {writer.write(response);writer.write("\r\n");writer.flush();}
        @Override public void close() throws Exception {
            if(closed.compareAndSet(false,true)){server.close();worker.shutdownNow();assertTrue(worker.awaitTermination(3,TimeUnit.SECONDS));}
        }
    }
}
