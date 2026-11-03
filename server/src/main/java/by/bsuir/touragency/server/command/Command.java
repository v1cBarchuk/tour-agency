package by.bsuir.touragency.server.command;

import by.bsuir.touragency.common.exception.AppException;
import by.bsuir.touragency.common.protocol.Request;
import by.bsuir.touragency.common.protocol.Response;
import by.bsuir.touragency.server.network.ClientSession;

/**
 * Команда сервера — обработчик одного типа запроса (паттерн Command).
 */
public interface Command {

    Response execute(Request request, ClientSession session) throws AppException;
}
