package daniil.pavelev.incident.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class OrderedTextItem {

    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "text", nullable = false, columnDefinition = "TEXT")
    private String text;

    protected OrderedTextItem() {
    }

    public OrderedTextItem(int position, String text) {
        this.position = position;
        this.text = text;
    }

    public int getPosition() {
        return position;
    }

    public String getText() {
        return text;
    }
}
