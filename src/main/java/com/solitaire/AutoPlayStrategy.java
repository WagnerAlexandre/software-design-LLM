package solitaire;

public interface AutoPlayStrategy {
    SuggestedMove decide(ReadOnlyGame game);
}
