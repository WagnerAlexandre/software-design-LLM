package solitaire;

public record SuggestedMove(Type type, LocationId source, Card card, LocationId target) {
    public enum Type { DRAW, MOVE, NONE }

    public static SuggestedMove draw() { return new SuggestedMove(Type.DRAW, null, null, null); }
    public static SuggestedMove move(LocationId source, Card card, LocationId target) {
        return new SuggestedMove(Type.MOVE, source, card, target);
    }
    public static SuggestedMove none() { return new SuggestedMove(Type.NONE, null, null, null); }
}
