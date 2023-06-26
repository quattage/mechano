package com.quattage.mechano.content.block.power.transfer.voltometer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.jozufozu.flywheel.backend.instancing.InstancedRenderDispatcher;
import com.mrh0.createaddition.energy.IWireNode;
import com.mrh0.createaddition.energy.LocalNode;
import com.mrh0.createaddition.energy.NodeRotation;
import com.mrh0.createaddition.energy.WireType;
import com.mrh0.createaddition.energy.network.EnergyNetwork;
import com.mrh0.createaddition.network.EnergyNetworkPacket;
import com.mrh0.createaddition.network.IObserveTileEntity;
import com.mrh0.createaddition.network.ObservePacket;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.core.util.MechanoLang;
import com.quattage.mechano.registry.MechanoBlocks;
import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.base.IRotate.StressImpact;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.utility.Color;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.LangBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public class VoltometerBlockEntity extends SmartBlockEntity implements IWireNode, IHaveGoggleInformation, IObserveTileEntity  {

    private final Set<LocalNode> wireCache = new HashSet<>();
    private final LocalNode[] nodes;
    private final IWireNode[] nodeCache;

    private boolean wasContraption = false;
    private boolean firstTick = true;

    private EnergyNetwork networkIn;
	private EnergyNetwork networkOut;
	
    private int demand = 0;
	private int throughput = 0;

    public float dialState = 0;
    public float prevDialState = 0;
    private float dialTarget = 0;

    private final int NODE_COUNT = 2;

    public int color;

    public static Vec3 OFFSET_NORTH = new Vec3(-0.76f, 0, 0);
	public static Vec3 OFFSET_WEST = new Vec3(0 , 0, -0.76f);
	public static Vec3 OFFSET_SOUTH = new Vec3(0.76f, 0, 0);
	public static Vec3 OFFSET_EAST = new Vec3(0, 0, 0.76f);

    public VoltometerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.nodes = new LocalNode[getNodeCount()];
		this.nodeCache = new IWireNode[getNodeCount()];
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    @Override
    public int getNodeCount() {
        return NODE_COUNT;
    }

    @Override
    public @Nullable IWireNode getWireNode(int index) {
        return IWireNode.getWireNodeFrom(index, this, this.nodes, this.nodeCache, level);
    }

    @Override
    public @Nullable LocalNode getLocalNode(int index) {
        return this.nodes[index];
    }

    @Override
    public int getAvailableNode(Vec3 pos) {
		pos = pos.subtract(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
        Direction facing = getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        boolean isTargetingOutput = true;

        switch(facing) {
            case NORTH -> isTargetingOutput = pos.x() < 0.5d;
            case WEST -> isTargetingOutput = pos.z() > 0.5d;
            case SOUTH -> isTargetingOutput = pos.x() > 0.5d;
            case EAST -> isTargetingOutput = pos.z() < 0.5d;
            default -> {}
        }
        if(isTargetingOutput) {
            if(!hasConnection(1)) return 1;
        } else {
            if(!hasConnection(0)) return 0;
        }
        return -1;
    }

    @Override
    public void setNode(int index, int other, BlockPos pos, WireType type) {
        this.nodes[index] = new LocalNode(this, index, other, type, pos);

		notifyUpdate();

		if (networkIn != null) networkIn.invalidate();
		if (networkOut != null) networkOut.invalidate();
    }

    @Override
    public void removeNode(int index, boolean dropWire) {
        LocalNode old = this.nodes[index];
		this.nodes[index] = null;

		invalidateNodeCache();
		notifyUpdate();

		if (networkIn != null) networkIn.invalidate();
		if (networkOut != null) networkOut.invalidate();
		if (dropWire && old != null) this.wireCache.add(old);
    }

    @Override
    public EnergyNetwork getNetwork(int node) {
        return isNodeInput(node) ? networkIn : networkOut;
    }

    @Override
    public void setNetwork(int node, EnergyNetwork network) {
        if(isNodeInput(node)) 
			networkIn = network;
		if(isNodeOutput(node))
			networkOut = network;
    }

    @Override
    public Vec3 getNodeOffset(int node) {
        Direction facing = getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        if(facing.getAxis() == Direction.Axis.X) facing = facing.getOpposite();
        //OUTPUT node
        if(node != 0) {
            return switch(facing) {
                case NORTH -> OFFSET_NORTH;
                case EAST -> OFFSET_EAST;
                case SOUTH -> OFFSET_SOUTH;
                case WEST -> OFFSET_WEST;
                default -> throw new IllegalArgumentException("Unexpected value: " + facing);
            };
        }
        // INPUT node
        return switch(facing) {
            case NORTH -> OFFSET_SOUTH;
            case EAST -> OFFSET_WEST;
            case SOUTH -> OFFSET_NORTH;
            case WEST -> OFFSET_EAST;
            default -> throw new IllegalArgumentException("Unexpected value: " + facing);
        };
    }

    @Override
    public BlockPos getPos() {
        return getBlockPos();
    }

    public int getDemand() {
		return demand;
	}

    public int getThroughput() {
		return throughput;
	}

    public void invalidateLocalNodes() {
		for(int i = 0; i < getNodeCount(); i++)
			this.nodes[i] = null;
	}

    @Override
    public void invalidateNodeCache() {
        for(int i = 0; i < getNodeCount(); i++)
			this.nodeCache[i] = null;
    }

    private void validateNodes() {
		boolean changed = validateLocalNodes(this.nodes);

		// Always set as changed if we were a contraption, as nodes might have been rotated.
		notifyUpdate();

		if (changed) {
			invalidateNodeCache();
			// Invalidate
			if (networkIn != null) networkIn.invalidate();
			if (networkOut != null) networkOut.invalidate();
		}
	}

    @Override
	public boolean isNodeInput(int node) {
		return node == 0;
	}
	
	@Override
	public boolean isNodeOutput(int node) {
		return !isNodeInput(node);
	}

    @Override
	public boolean isNodeIndeciesConnected(int in, int other) {
		return isNodeInput(in) == isNodeInput(other);
    }

    @Override
	public void write(CompoundTag nbt, boolean clientPacket) {
		super.write(nbt, clientPacket);
		// Write nodes.
		ListTag nodes = new ListTag();
		for (int i = 0; i < getNodeCount(); i++) {
			LocalNode localNode = this.nodes[i];
			if (localNode == null) continue;
			CompoundTag tag = new CompoundTag();
			localNode.write(tag);
			nodes.add(tag);
		}
        nbt.putFloat("DialTarget", dialTarget);
        nbt.putFloat("DialState", dialState);
        nbt.putFloat("OldState", prevDialState);
        nbt.putInt("Color", color);
		nbt.put(LocalNode.NODES, nodes);
	}

    @Override
    protected void read(CompoundTag nbt, boolean clientPacket) {
		// Convert old nbt data. x0, y0, z0, node0 & type0 etc.
		if (!clientPacket && nbt.contains("node0")) {
			convertOldNbt(nbt);
			setChanged();
		}

		// Read the nodes.
		invalidateLocalNodes();
		invalidateNodeCache();
		ListTag nodes = nbt.getList(LocalNode.NODES, Tag.TAG_COMPOUND);
		nodes.forEach(tag -> {
			LocalNode localNode = new LocalNode(this, (CompoundTag) tag);
			this.nodes[localNode.getIndex()] = localNode;
		});

		// Check if this was a contraption.
		if (nbt.contains("contraption") && !clientPacket) {
			this.wasContraption = nbt.getBoolean("contraption");
			NodeRotation rotation = getBlockState().getValue(NodeRotation.ROTATION);
			if (rotation != NodeRotation.NONE)
				level.setBlock(getBlockPos(), getBlockState().setValue(NodeRotation.ROTATION, NodeRotation.NONE), 0);
			// Loop over all nodes and update their relative positions.
			for (LocalNode localNode : this.nodes) {
				if (localNode == null) continue;
				localNode.updateRelative(rotation);
			}
		}

		// Invalidate the network if we updated the nodes.
		if (!nodes.isEmpty() && this.networkIn != null && this.networkOut != null) {
			this.networkIn.invalidate();
			this.networkOut.invalidate();
		}

        dialTarget = nbt.getFloat("DialTarget");
        prevDialState = nbt.getFloat("OldState");
        dialState = nbt.getFloat("DialState");
        color = nbt.getInt("Color");
        super.read(nbt, clientPacket);

        if (clientPacket)
			DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> InstancedRenderDispatcher.enqueueUpdate(this));
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
        super.onDataPacket(connection, packet);
        this.load(packet.getTag());
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag nbt = super.getUpdateTag();
        nbt.putFloat("DialTarget", dialTarget);
        nbt.putFloat("DialState", dialState);
        nbt.putFloat("OldState", prevDialState);
        nbt.putInt("Color", color);
        return nbt;
    }

    /***
     * Derives a degree (2 -> 178) from a percent of two numbers
     * @param throughput
     * @param demand
     * @return
     */
    private float calculateTarget(int throughput, int demand) {
        float dialState;
        if(throughput == 0 || demand == 0) dialState = 0;
        else {
            dialState = ((float)throughput / (float)demand) * 180;
            if(dialState < 2) dialState = 2;
            if(dialState > 178) dialState = 178;
        }
        
        return dialState;
    }

    @Override
	public void tick() {
        super.tick();
        prevDialState = dialState;
        dialTarget = calculateTarget(throughput, demand);
        dialState += ((dialTarget - dialState) / 8);

		if (this.firstTick) {
			this.firstTick = false;
			if (this.wasContraption && !level.isClientSide()) {
				this.wasContraption = false;
				validateNodes();
			}
		}

		if (!this.wireCache.isEmpty() && !isRemoved()) handleWireCache(level, this.wireCache);

		if (level.isClientSide()) return;
		networkTick();

        if(prevDialState != dialState) {
            if (dialTarget > 0) {
                if (dialTarget < .5f)
                    color = Color.mixColors(0x00FF00, 0xFFFF00, dialTarget * 2);
                else if (dialTarget < 1)
                    color = Color.mixColors(0xFFFF00, 0xFF0000, (dialTarget) * 2 - 1);
                else
                    color = 0xFF0000;
            }
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
            this.setChanged();
        }
	}

	private void networkTick() {
		if(awakeNetwork(level)) notifyUpdate();
		BlockState bs = getBlockState();

		if(!bs.is(MechanoBlocks.VOLTOMETER.get())) {
            throughput = 0;
            return;
        }

        throughput = networkOut.push(networkIn.pull(demand));
        demand = networkIn.demand(networkOut.getDemand());
	}

    @Override
	public void remove() {
		if (level.isClientSide()) return;
		for (int i = 0; i < getNodeCount(); i++) {
			LocalNode localNode = getLocalNode(i);
			if (localNode == null) continue;
			IWireNode otherNode = getWireNode(i);
			if (otherNode == null) continue;

			int ourNode = localNode.getOtherIndex();
			if (localNode.isInvalid()) otherNode.removeNode(ourNode);
			else otherNode.removeNode(ourNode, true);
		}
		invalidateNodeCache();
		invalidateCaps();

		// Invalidate
		if (networkIn != null) networkIn.invalidate();
		if (networkOut != null) networkOut.invalidate();
	}

    @Override
	public void onObserved(ServerPlayer player, ObservePacket pack) {
		if(isNetworkValid(pack.getNode()))
			EnergyNetworkPacket.send(worldPosition, demand, throughput, player);
	}

    @Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		@SuppressWarnings("resource")
		HitResult ray = Minecraft.getInstance().hitResult;
		if(ray == null)
			return false;
		int node = getAvailableNode(ray.getLocation());
		
		ObservePacket.send(worldPosition, node);
        int inputThrough = (int)EnergyNetworkPacket.clientBuff;
        int outputDemand = (int)EnergyNetworkPacket.clientDemand;

        float ioFrac = inputThrough / (outputDemand == 0 ? 1 : outputDemand);
		
		//tooltip.add(componentSpacing.plainCopy().append(Lang.translateDirect("gui.gauge.info_header")));
        Mechano.logSlow("DEMAND: " + (int)EnergyNetworkPacket.clientDemand + "  THROUGHPUT: " +(int)EnergyNetworkPacket.clientBuff);

        MechanoLang.translate("gui.voltometer.info_header")
            .forGoggles(tooltip);
        if(inputThrough == 0) {
            MechanoLang.text(TooltipHelper.makeProgressBar(3, 0))
                .translate("gui.voltometer.no_power")
                .style(ChatFormatting.DARK_GRAY)
                .forGoggles(tooltip);

        } else {
            StressImpact.getFormattedStressText(ioFrac)
				.forGoggles(tooltip);
			Lang.translate("gui.stressometer.capacity")
				.style(ChatFormatting.GRAY)
				.forGoggles(tooltip);

			double remainingCapacity = outputDemand - inputThrough;

			LangBuilder su = Lang.translate("generic.unit.stress");
			LangBuilder stressTip = Lang.number(remainingCapacity)
				.add(su)
				.style(StressImpact.of(ioFrac)
					.getRelativeColor());
        }
        

		return true;
    }
}
