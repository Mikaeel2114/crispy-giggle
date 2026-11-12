package net.md_5.bungee.api.chat;

public class TextComponent extends BaseComponent {

    public TextComponent() {
    }

    public TextComponent(String text) {
    }

    public TextComponent(TextComponent textComponent) {
    }

    public TextComponent(BaseComponent... extras) {
    }

    public static BaseComponent[] fromLegacyText(String message) {
        return new BaseComponent[]{new TextComponent(message)};
    }
}
