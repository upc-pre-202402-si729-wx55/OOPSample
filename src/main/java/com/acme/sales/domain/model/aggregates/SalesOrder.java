package com.acme.sales.domain.model.aggregates;

import com.acme.shared.domain.model.valueobjects.Address;

import java.util.List;
import java.util.UUID;

public class SalesOrder {
    private final UUID internalId;
    private Address shippingAddress;
    private SalesOrderStatus status;
    private List<SalesOrderItem> items;
    private double paymentAmount;

    public SalesOrder() {
        this.internalId = UUID.randomUUID();
        this.status = SalesOrderStatus.CREATED;
        paymentAmount = 0.0;
    }

    public void addItem(int quantity, double unitPrice, Long productId) {
        if (this.status == SalesOrderStatus.APPROVED)
            throw new IllegalStateException("Cannot add items to and approved order");
        this.items.add(new SalesOrderItem(quantity, productId, unitPrice));
    }

    public double calculateTotalPrice() {
        return items.stream().mapToDouble(SalesOrderItem::calculatePrice).sum();
    }

    public void addPayment(double amount) {
        if (amount <= 0)
            throw new IllegalArgumentException("Payment amount must be greater than zero");

        if (amount > calculateTotalPrice() - paymentAmount)
            throw new IllegalArgumentException("Payment amount exceeds the total price");
        this.paymentAmount += amount;
        verifyIfReadyForDispatch();
    }

    public void dispatch(String street, String number, String city, String state, String zipCode, String country) {
        verifyIfReadyForDispatch();
        this.shippingAddress = new Address(street, number, city, state, zipCode, country);
        this.status = SalesOrderStatus.IN_PROGRESS;
        this.items.forEach(SalesOrderItem::dispatch);
    }

    public boolean isDispatched() {
        return this.items.stream().allMatch(SalesOrderItem::isDispatched);
    }

    public void verifyIfItemsAreDispatched() {
        if (isDispatched()) this.status = SalesOrderStatus.SHIPPED;
    }

    public void completeDelivery() {
        verifyIfItemsAreDispatched();
        if (status == SalesOrderStatus.SHIPPED) status = SalesOrderStatus.DELIVERED;
    }

    public void cancel() {
        clearItems();
        status = SalesOrderStatus.CANCELLED;
    }

    public String getShippingAddressAsString() {
        return shippingAddress.getAddressAsString();
    }

    private UUID getInternalId() { return internalId; }

    private void verifyIfReadyForDispatch() {
        if (this.status == SalesOrderStatus.APPROVED) return;
        if (paymentAmount == calculateTotalPrice()) this.status = SalesOrderStatus.APPROVED;
    }

    private void clearItems() { this.items.clear();}


}
