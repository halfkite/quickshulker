package net.kyrptonaught.quickshulker.config;
import net.kyrptonaught.kyrptconfig.config.screen.items.ConfigItem;
import net.kyrptonaught.kyrptconfig.config.screen.NotSuckyButton;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
/** Creative-only, unrestricted, and disabled are one permission selector, not two unrelated toggles. */
public final class ContainerModeItem extends ConfigItem<Integer>{
    private final NotSuckyButton button;
    public ContainerModeItem(ConfigOptions config){
        this(config,null);
    }
    public ContainerModeItem(ConfigOptions config,ContainerKind kind){
        super(Component.translatable("key.quickshulker.config."+(kind==null?"quickContainers":kind.key)),kind==null?mode(config):kind.mode(config),kind==null?0:1);
        button=new NotSuckyButton(0,0,100,20,Component.empty(),b->setValue((value+1)%3));
        setValue(value);useDefaultResetBTN();
        setSaveConsumer(v->{if(kind!=null)kind.setMode(config,v);else{config.quickContainers=v!=2;config.quickContainersCreativeOnly=v==0;}});
        setToolTip(Component.translatable("quickshulker.config.server_policy"));
    }
    public static int mode(ConfigOptions c){return !c.quickContainers?2:c.quickContainersCreativeOnly?0:1;}
    @Override public void setValue(Integer mode){
        super.setValue(mode);
        button.setMessage(mode==0?Component.translatable("quickshulker.config.creative"):Component.literal(mode==1?"true":"false"));
        button.setButtonColor((mode==0?DyeColor.YELLOW:mode==1?DyeColor.LIME:DyeColor.RED).getTextColor());
    }
    public void mouseClicked(MouseButtonEvent click,boolean doubled){super.mouseClicked(click,doubled);button.mouseClicked(click,doubled);}
    public void extractRenderState(GuiGraphicsExtractor context,int x,int y,int mouseX,int mouseY,float delta){
        super.extractRenderState(context,x,y,mouseX,mouseY,delta);button.setY(y);
        button.setX(resetButton.getX()-resetButton.getWidth()-(button.getWidth()/2)-20);
        button.extractRenderState(context,mouseX,mouseY,delta);
    }
}
