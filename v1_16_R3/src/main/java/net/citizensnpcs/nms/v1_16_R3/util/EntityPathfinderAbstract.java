package net.citizensnpcs.nms.v1_16_R3.util;

import org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import net.minecraft.server.v1_16_R3.BlockPosition;
import net.minecraft.server.v1_16_R3.ChunkCache;
import net.minecraft.server.v1_16_R3.EntityLiving;
import net.minecraft.server.v1_16_R3.MathHelper;
import net.minecraft.server.v1_16_R3.PathPoint;
import net.minecraft.server.v1_16_R3.PathfinderAbstract;

public abstract class EntityPathfinderAbstract extends PathfinderAbstract {
    protected ChunkCache a;
    protected EntityLiving b;
    protected final Int2ObjectMap<PathPoint> c = new Int2ObjectOpenHashMap();
    protected int d;
    protected int e;
    protected int f;
    protected boolean g;
    protected boolean h;
    protected boolean i;
    protected MobAI mvmt;

    @Override
    public void a() {
        this.a = null;
        this.b = null;
        mvmt = null;
    }

    @Override
    protected PathPoint a(BlockPosition var0) {
        return a(var0.getX(), var0.getY(), var0.getZ());
    }

    @Override
    public void a(boolean var0) {
        this.g = var0;
    }

    public void a(ChunkCache var0, EntityLiving var1) {
        this.mvmt = MobAI.from(var1);
        this.a = var0;
        this.b = var1;
        this.c.clear();
        this.d = MathHelper.d(var1.getWidth() + 1.0F);
        this.e = MathHelper.d(var1.getHeight() + 1.0F);
        this.f = MathHelper.d(var1.getWidth() + 1.0F);
    }

    @Override
    protected PathPoint a(int var0, int var1, int var2) {
        return this.c.computeIfAbsent(PathPoint.b(var0, var1, var2), var3 -> new PathPoint(var0, var1, var2));
    }

    @Override
    public void b(boolean var0) {
        this.h = var0;
    }

    @Override
    public boolean c() {
        return this.g;
    }

    @Override
    public void c(boolean var0) {
        this.i = var0;
    }

    @Override
    public boolean d() {
        return this.h;
    }

    @Override
    public boolean e() {
        return this.i;
    }
}
