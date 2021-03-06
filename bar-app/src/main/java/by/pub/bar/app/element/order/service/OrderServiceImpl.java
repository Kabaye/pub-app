package by.pub.bar.app.element.order.service;

import by.pub.bar.app.element.ingredient.entity.Ingredient;
import by.pub.bar.app.element.ingredient.service.IngredientService;
import by.pub.bar.app.element.order.entity.Order;
import by.pub.bar.app.element.order.repository.OrderRepository;
import by.pub.bar.app.element.order.utils.OrderDBProcessor;
import by.pub.bar.app.element.product.service.ProductService;
import by.pub.bar.app.event.entity.NewOrderSavedEvent;
import by.pub.bar.app.event.publisher.BarEventPublisher;
import by.pub.bar.app.utils.Status;
import by.pub.bar.app.web.client.web_client.WebClient;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductService productService;
    private final IngredientService ingredientService;
    private final OrderDBProcessor processor;
    private final WebClient webClient;
    private final BarEventPublisher publisher;

    public OrderServiceImpl(OrderRepository orderRepository, ProductService productService,
        IngredientService ingredientService, OrderDBProcessor processor, WebClient webClient,
        BarEventPublisher publisher) {
        this.orderRepository = orderRepository;
        this.productService = productService;
        this.ingredientService = ingredientService;
        this.processor = processor;
        this.webClient = webClient;
        this.publisher = publisher;
    }

    @Override
    public List<Order> findAllOrders() {
        return orderRepository.findAll()
            .stream()
            .map(processor::fromDB)
            .collect(Collectors.toList());
    }

    @Override
    public Order saveOrder(Order order) {
        order.setProducts(order.getProducts().stream()
            .map(product -> productService.findByName(Objects.requireNonNull(product.getName())))
            .collect(Collectors.toList()));
        final Order savedOrder = processor
            .fromDB(orderRepository.save(processor.toDB(processor.preprocessTotalPrice(order))));
        publisher.publishEvent(new NewOrderSavedEvent(savedOrder));
        return savedOrder;
    }

    @Override
    public void deleteOrderById(String id) {
        orderRepository.deleteById(id);
    }

    @Override
    public Order acceptOrder(Order order) {
        if (!order.getProducts().stream()
            .flatMap(simpleProduct -> simpleProduct.getUsedIngredients().stream())
            .allMatch(ingredientService::checkForAvailability)) {
            throw new RuntimeException(
                "We don't have some ingredients for accepting this order. Request some from storage!");
        }

        order.getProducts().stream()
            .flatMap(simpleProduct -> simpleProduct.getUsedIngredients().stream())
            .forEach(simpleIngredient -> ingredientService
                .takeIngredientFromBarStand(Ingredient.of(simpleIngredient)));

        webClient.sendAcceptedOrder(order.setStatus(Status.ACCEPTED));
        deleteOrderById(order.getId());
        return order;
    }
}
