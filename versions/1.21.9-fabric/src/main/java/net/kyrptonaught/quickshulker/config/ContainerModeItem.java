package net.kyrptonaught.quickshulker.config;
import net.kyrptonaught.kyrptconfig.config.screen.items.ConfigItem;
import net.kyrptonaught.kyrptconfig.config.screen.NotSuckyButton;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
public final class ContainerModeItem extends ConfigItem<Integer> {
    private final NotSuckyButton button;
    public ContainerModeItem(ConfigOptions config) {
        super(Text.translatable("key.quickshulker.config.quickContainers"),!config.quickContainers?2:config.quickContainersCreativeOnly?0:1,0);
        button=new NotSuckyButton(0,0,100,20,Text.empty(),b->setValue((value+1)%3));
        setValue(value);useDefaultResetBTN();
        setSaveConsumer(v->{config.quickContainers=v!=2;config.quickContainersCreativeOnly=v==0;});
        setToolTip(Text.translatable("quickshulker.config.server_policy"));
    }
    @Override public void setValue(Integer mode) {
        super.setValue(mode);
        button.setMessage(mode==0?Text.translatable("quickshulker.config.creative"):Text.literal(mode==1?"true":"false"));
        button.setButtonColor((mode==0?DyeColor.YELLOW:mode==1?DyeColor.LIME:DyeColor.RED).getSignColor());
    }
    public void mouseClicked(net.minecraft.client.gui.Click click,boolean doubled){super.mouseClicked(click,doubled);button.mouseClicked(click,doubled);}
    public void render(DrawContext g,int x,int y,int mx,int my,float d){
        super.render(g,x,y,mx,my,d);button.setY(y);
        button.setX(resetButton.getX()-resetButton.getWidth()-button.getWidth()/2-20);
        button.render(g,mx,my,d);
    }
}
