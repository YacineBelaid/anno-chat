package com.rls.chat.messages;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.rls.chat.auth.AuthController;
import com.rls.chat.auth.session.SessionData;
import com.rls.chat.auth.session.SessionManager;
import com.rls.chat.messages.model.Message;
import com.rls.chat.messages.model.NewMessageRequest;
import com.rls.chat.messages.repository.MessageRepository;
import com.rls.chat.websocket.WebSocketManager;

/**
 * Contrôleur qui gère l'API de messages.
 */
@RestController
public class MessageController {
    public static final String MESSAGES_PATH = "/messages";

    private final MessageRepository messageRepository;
    private final WebSocketManager webSocketManager;
    private final SessionManager sessionManager;

    public MessageController(MessageRepository messageRepository,
            WebSocketManager webSocketManager, SessionManager sessionManager) {
        this.messageRepository = messageRepository;
        this.webSocketManager = webSocketManager;
        this.sessionManager = sessionManager;
    }

    @GetMapping(MESSAGES_PATH)
    public List<Message> getMessages(@RequestParam Optional<String> fromId)
            throws InterruptedException, ExecutionException {
        try {
            return this.messageRepository.getMessages(fromId.orElse(null));
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur interne au serveur lors de la récupération des messages.");
        }
    }

    @PostMapping(MESSAGES_PATH)
    public Message createMessage(@CookieValue(AuthController.SESSION_ID_COOKIE_NAME) String sessionCookie,
            @RequestBody NewMessageRequest message)
            throws InterruptedException, ExecutionException {
        try {
            if (sessionCookie == null || sessionCookie.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            SessionData sessionData = this.sessionManager.getSession(sessionCookie);
            if (sessionData == null || !sessionData.username().equals(message.username())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            Message newMessage = this.messageRepository.createMessage(message);

            this.webSocketManager.notifySessions();

            return newMessage;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur interne au serveur lors de la creation d'un Message.");
        }

    }
}
