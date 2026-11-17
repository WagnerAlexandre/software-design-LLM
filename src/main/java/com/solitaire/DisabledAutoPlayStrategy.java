package solitaire;

public final class DisabledAutoPlayStrategy implements AutoPlayStrategy {
    @Override
    public SuggestedMove decide(ReadOnlyGame game) {
        return SuggestedMove.none();
    }
}
