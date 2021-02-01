public class MinecraftThrowedException extends RuntimeException{
    public MinecraftThrowedException(Throwable e){
        super("An exception was thrown in Minecraft, see Caused By!", e);
    }
}
