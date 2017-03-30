package crazypants.enderzoo.entity;

import crazypants.enderzoo.config.Config;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.EntityMagmaCube;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class EntityDireSlime extends EntityMagmaCube implements IEnderZooMob {

  public static final String NAME = "DireSlime";
  public static final int EGG_BG_COL = 0xb9855c;
  public static final int EGG_FG_COL = 0x593d29;

  public enum SlimeConf {

    SMALL(1, Config.direSlimeHealth, Config.direSlimeAttackDamage, 1 - (Config.direSlimeChanceLarge - Config.direSlimeChanceMedium)), MEDIUM(2,
        Config.direSlimeHealthMedium, Config.direSlimeAttackDamageMedium, Config.direSlimeChanceMedium), LARGE(4, Config.direSlimeHealthLarge,
        Config.direSlimeAttackDamageLarge, Config.direSlimeChanceLarge);

    public final int size;
    public final double health;
    public final double attackDamage;
    public final double chance;

    private SlimeConf(int size, double health, double attackDamage, double chance) {
      this.size = size;
      this.health = health;
      this.attackDamage = attackDamage;
      this.chance = chance;
    }

    static SlimeConf getConfForSize(int size) {
      for (SlimeConf conf : values()) {
        if (conf.size == size) {
          return conf;
        }
      }
      return SMALL;
    }

    SlimeConf bigger() {
      int index = ordinal() + 1;
      if (index >= values().length) {
        return null;
      }
      return values()[index];
    }
  }

  public EntityDireSlime(World world) {
    super(world);
    setSlimeSize(1,false);
  }

  @Override
  public void setSlimeSize(int size,boolean doFullHeal) {
    super.setSlimeSize(size,doFullHeal);
    SlimeConf conf = SlimeConf.getConfForSize(size);
    getAttributeMap().getAttributeInstance(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(conf.attackDamage);
    getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(conf.health);
    setHealth(getMaxHealth());
  }

  @Override
  public void onDeath(DamageSource damageSource) {
    super.onDeath(damageSource);
    if (!world.isRemote && damageSource != null && damageSource.getEntity() instanceof EntityPlayer) {
      SlimeConf nextConf = SlimeConf.getConfForSize(getSlimeSize()).bigger();
      if (nextConf != null && world.rand.nextFloat() <= nextConf.chance) {
        EntityDireSlime spawn = new EntityDireSlime(world);
        spawn.setSlimeSize(nextConf.size,true);
        spawn.setLocationAndAngles(posX, posY, posZ, rotationYaw, 0);
        spawn.onInitialSpawn(world.getDifficultyForLocation(new BlockPos(this)), null);
        if (SpawnUtil.isSpaceAvailableForSpawn(world, spawn, false)) {
          world.spawnEntity(spawn);
        }
      }
    }
  }

  @Override
  public void setDead() {
    //Override to prevent smaller slimes spawning
    isDead = true;
  }

//  @Override
//  protected String getSlimeParticle() {
//    return "blockcrack_" + Block.getIdFromBlock(Blocks.dirt) + "_0";
//  }

  @Override
  protected EntitySlime createInstance() {
    return new EntityDireSlime(this.world);
  }

  @Override
  protected Item getDropItem() {
    return Items.CLAY_BALL;
  }

  @Override
  @SideOnly(Side.CLIENT)
  public int getBrightnessForRender(float p_70070_1_) {
    int i = MathHelper.floor(this.posX);
    int j = MathHelper.floor(this.posZ);

    if (!world.isAirBlock(new BlockPos(i, 0, j))) {
      double d0 = (getEntityBoundingBox().maxY - getEntityBoundingBox().minY) * 0.66D;
      int k = MathHelper.floor(this.posY - getYOffset()+ d0);
      return world.getCombinedLight(new BlockPos(  i, k, j), 0);           
    } else {
      return 0;
    }
  }

  @Override
  public float getBrightness(float p_70013_1_) {
    int i = MathHelper.floor(this.posX);
    int j = MathHelper.floor(this.posZ);

    if (!world.isAirBlock(new BlockPos(i, 0, j))) {
      double d0 = (getEntityBoundingBox().maxY - getEntityBoundingBox().minY) * 0.66D;
      int k = MathHelper.floor(this.posY - getYOffset() + d0);
      return world.getLightBrightness(new BlockPos(  i, k, j));
    } else {
      return 0.0F;
    }
  }

  @Override
  protected void applyEntityAttributes() {
    super.applyEntityAttributes();
    getAttributeMap().registerAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
  }

  @Override
  protected int getAttackStrength() {
    int res = (int) getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue();
    return res;
  }

  // This is called every tick on onUpdate(), so avoid moving the slime around twice per tick.
  @Override
  protected void setSize(float p_70105_1_, float p_70105_2_) {
    int i = this.getSlimeSize();
    super.setSize(i, i);
  }

  @Override
  public void onCollideWithPlayer(EntityPlayer p_70100_1_) {
    int i = getSlimeSize();
    if (canEntityBeSeen(p_70100_1_) && this.getDistanceSqToEntity(p_70100_1_) < (double) i * (double) i
        && p_70100_1_.attackEntityFrom(DamageSource.causeMobDamage(this), getAttackStrength())) {
      playSound(SoundEvents.ENTITY_SLIME_ATTACK, 1.0F, (rand.nextFloat() - rand.nextFloat()) * 0.2F + 1.0F);
    }
  }
  
  @Override
  protected float applyArmorCalculations(DamageSource p_70655_1_, float p_70655_2_) {
      if (!p_70655_1_.isUnblockable()) {
        return Math.min(Math.max(p_70655_2_ - 3 - this.getSlimeSize(), this.getSlimeSize()) / 2, p_70655_2_);
      }
      return p_70655_2_;
  }

}
