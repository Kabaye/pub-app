package by.pub.bar.app.ingredient_request.service;

import by.pub.bar.app.event.entity.NewIngredientRequestEvent;
import by.pub.bar.app.event.publisher.BarEventPublisher;
import by.pub.bar.app.ingredient_request.entity.IngredientRequest;
import by.pub.bar.app.ingredient_request.repository.IngredientRequestRepository;
import by.pub.bar.app.websocket.client.CustomWebSocketClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class IngredientRequestServiceImpl implements IngredientRequestService {
    private final IngredientRequestRepository ingredientRequestRepository;
    private final BarEventPublisher publisher;
    private final CustomWebSocketClient webSocketClient;

    public IngredientRequestServiceImpl(IngredientRequestRepository ingredientRequestRepository, BarEventPublisher publisher,
                                        CustomWebSocketClient webSocketClient) {
        this.ingredientRequestRepository = ingredientRequestRepository;
        this.publisher = publisher;
        this.webSocketClient = webSocketClient;
    }

    @Override
    public IngredientRequest findByRequestId(String requestId) {
        return ingredientRequestRepository.findByRequestId(requestId)
                .orElseThrow(() -> new RuntimeException("There is no Ingredient request with request id: " + requestId));
    }

    @Override
    public List<IngredientRequest> findAllIngredientRequests() {
        return ingredientRequestRepository.findAll();
    }

    @Override
    public void deleteByRequestId(String requestId) {
        ingredientRequestRepository.deleteByRequestId(requestId);
    }

    @Override
    public void createAndSendIngredientRequest(String ingredientName, Long amount) {
        IngredientRequest ingredientRequest = ingredientRequestRepository.save(new IngredientRequest()
                .setIngredientAmount(amount)
                .setIngredientName(ingredientName)
                .setRequestId(UUID.randomUUID().toString()));

        publisher.publishEvent(new NewIngredientRequestEvent(ingredientRequest));

        webSocketClient.requestIngredient(ingredientRequest);
    }

    @Override
    public void acceptIngredientRequest(IngredientRequest ingredientRequest) {
        ingredientRequestRepository.deleteByRequestId(ingredientRequest.getRequestId());
    }
}