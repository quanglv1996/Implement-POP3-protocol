/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bkhn.it3080.group9.pop3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 *
 * @author lequangbkhn
 */
public class POP3Session {
// Thoi gian dong Socket

    public static final int SOCKET_READ_TIMEOUT = 30 * 1000;

    protected SSLSocket pop3Socket;
    protected BufferedReader in;
    protected PrintWriter out;

    private String host;
    private int port;
    private String userName;
    private String password;

    // Kiem tra tra loi cua server
    // Neu thanh cong tra ve +OK Neu that bai tra lai -ERR
    protected void checkForError(String response) throws IOException {
        if (response.charAt(0) != '+') {
            throw new IOException(response);
        }
    }

    // Gui lenh neu +OK thi tra lai toan bo tra loi Neu sai thi nem ra Exception
    protected String doCommand(String command) throws IOException {
        System.out.println("C : " + command);
        out.println(command);
        out.flush();
        String response = in.readLine();
        checkForError(response);
        System.out.println("S : " + response);
        return response;
    }

    // Tra loi multilines
    public String[] getMultilineResponse() throws IOException {
        ArrayList lines = new ArrayList();

        while (true) {
            String line = in.readLine();
            System.out.println(line);
            if (line == null) {
                throw new IOException("Server unawares closed the connection.");
            }
            if (line.equals(".")) {
                break;
            }
            if ((line.length() > 0) && (line.charAt(0) == '.')) {
                System.out.println(line);
                line = line.substring(1);
            }
            lines.add(line);
        }
        String response[] = new String[lines.size()];
        lines.toArray(response);
        return response;
    }

    // Khoi tao phien lam viec
    public POP3Session(String host, String userName, String password) {
        this(host, 995, userName, password);
    }

    public POP3Session(String host, int port, String userName, String password) {
        this.host = host;
        this.port = port;
        this.userName = userName;
        this.password = password;
    }

    // Tao ket noi va gui di USER/PASS trong trang thai xac thuc
    public void connectAndAuthenticate() throws IOException {
        SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        pop3Socket = (SSLSocket) sslsocketfactory.createSocket(host, port);
        pop3Socket.setSoTimeout(SOCKET_READ_TIMEOUT);
        in = new BufferedReader(new InputStreamReader(pop3Socket.getInputStream()));
        out = new PrintWriter(new OutputStreamWriter(pop3Socket.getOutputStream()));
        // Nhan ve tra loi cua server
        String response = in.readLine();
        checkForError(response);
        checkForError(commandUSER(userName));
        checkForError(commandPASS(password));

    }

    // Lay thong tin cua mail
    public Mail getInfoMail(String[] response) {

        Mail mail = new Mail();
        boolean flagFrom = false;
        boolean flagTo = false;
        boolean flagDate = false;
        boolean flagSubject = false;
        boolean flagContent = false;
        for (int i = 0; i < response.length; i++) {
            if (response[i].startsWith("From:") && !flagFrom) {
                int indexEnd = response[i].indexOf(" ", 10);
                String from = response[i].substring(indexEnd + 2, response[i].length() - 1);
                mail.setFrom(from);
                flagFrom = true;
                continue;
            }
            if (response[i].startsWith("To:") && !flagTo) {
                mail.setTo(response[i]);
                flagTo = true;
                continue;
            }
            if (response[i].startsWith("Date:") && !flagDate) {
               mail.setDate(response[i]);
                flagDate = true;
                continue;
            }
            if (response[i].startsWith("Subject:") && !flagSubject) {
                mail.setSubject(response[i]);
                flagSubject = true;
                continue;
            }
            if (response[i].startsWith("Content-Type: text/plain") && !flagContent) {
                StringBuffer sb = new StringBuffer("Content: ");
                int index = 0;
                for (int j = i + 1; j < response.length; j++) {
                    if (response[j].startsWith("Content-Type: text/html")) {
                        index = j - 1;
                        break;
                    }
                }
                for (int k = i + 1; k < index; k++) {
                    sb.append(response[k] + "\n");
                }
                flagContent = true;
                mail.setContent(sb.toString());
                continue;
            }
        }
        return mail;
    }

    // Lenh USER String
    public String commandUSER(String name) throws IOException {
        String reponse = new String();
        reponse = doCommand("USER " + name);
        return reponse;
    }

    // Lenh PASS String
    public String commandPASS(String pass) throws IOException {
        String reponse = new String();
        reponse = doCommand("PASS " + pass);
        return reponse;
    }

    // STAT tra ve (+OK msg_count size_in_bytes)
    // Lenh STAT (Lay so luong message)
    public int commandSTATCountMsg() throws IOException {
        String response = doCommand("STAT");
        try {
            int indexEnd = response.indexOf(' ', 4);
            String countStr = response.substring(4, indexEnd);
            int count = Integer.valueOf(countStr).intValue();
            return count;
        } catch (Exception e) {
            throw new IOException("Invalid response - " + response);
        }
    }

    // Lenh STAT(Lay kich thuoc mailbox)
    public int commandSTATSizeMailbox() throws IOException {
        String response = doCommand("STAT");
        try {
            int indexMid = response.indexOf(' ', 4);
            String sizeStr = response.substring(indexMid);
            int size = Integer.valueOf(sizeStr).intValue();
            return size;
        } catch (Exception e) {
            throw new IOException("Invalid response - " + response);
        }

    }

    // Lenh LIST
    public String[] commandLISTnotArgument() throws IOException {
        doCommand("LIST");
        return getMultilineResponse();
    }

    // Lenh LIST string
    public String commandLISThaveArgument(String messageId) throws IOException {
        String response = doCommand("LIST " + messageId);
        return response;
    }

    // Lenh RETR
    public String commandRETR(String messageId) throws IOException {
        doCommand("RETR " + messageId);
        String[] messageLines = getMultilineResponse();
        StringBuffer message = new StringBuffer();
        for (int i = 0; i < messageLines.length; i++) {
            message.append(messageLines[i]);
            message.append("\n");
        }
        return new String(message);
    }

    public String[] commandRETRresponse(String messageId) throws IOException {
        doCommand("RETR " + messageId);
        return getMultilineResponse();

    }

    // Lenh TOP string int
    public String[] commandTOP(String messageId, int lineCount) throws IOException {
        doCommand("TOP " + messageId + " " + lineCount);
        return getMultilineResponse();
    }

    // Lenh NOOP
    public String commandNOOP() throws IOException {
        String response = doCommand("NOOP");
        return response;
    }

    // Lenh RESET
    public void commandRESET() throws IOException {
        String reponse = doCommand("RSET");
    }

    // Lenh DELE
    public void commandDELE(String messageId) throws IOException {
        String response = doCommand("DELE " + messageId);
    }

    // Lenh QUIT
    public void commandQUIT() throws IOException {
        String response = doCommand("QUIT");
    }

    // Dong ket noi
    public void close() {
        try {
            in.close();
            out.close();
            pop3Socket.close();
        } catch (Exception ex) {
        }
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
