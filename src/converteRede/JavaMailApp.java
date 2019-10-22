package converteRede;

import java.util.Properties;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class JavaMailApp{
	Properties props;
	Session session;
      
	public JavaMailApp() {
		props= new Properties();
        /** Parâmetros de conexão com servidor Gmail */
        props.put("mail.smtp.host", "smtp.paulimetalurgica.com.br");
        props.put("mail.smtp.socketFactory.port", "587");
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", "587");
        session = Session.getDefaultInstance(props,
        		new javax.mail.Authenticator() {
	                protected PasswordAuthentication getPasswordAuthentication(){
	                	return new PasswordAuthentication("administrativo@paulimetalurgica.com.br", "Abcd6789");
	                }
                });
        /** Ativa Debug para sessão */
       // session.setDebug(true);
	}
	public void enviaEmail(String mensagem, String assunto) {
		try {
			Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress("suporte@paulimetalurgica.com.br")); //Remetente
            Address[] toUser = InternetAddress //Destinatário(s)
            		.parse("feliperamblings@gmail.com");  //emails separados por virgulas
            message.setRecipients(Message.RecipientType.TO, toUser);
            message.setSubject(assunto);//Assunto
            message.setText(mensagem);
            /**Método para enviar a mensagem criada*/
            Transport.send(message);
            System.out.println("Feito!!!");
             } catch (MessagingException e) {
                  throw new RuntimeException(e);
            }
      }
}