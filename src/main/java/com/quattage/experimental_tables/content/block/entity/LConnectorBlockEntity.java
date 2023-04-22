package com.quattage.experimental_tables.content.block.entity;

import java.util.List;

import com.mrh0.createaddition.CreateAddition;
import com.mrh0.createaddition.energy.BaseElectricTileEntity;
import com.mrh0.createaddition.energy.IWireNode;
import com.mrh0.createaddition.energy.WireType;
import com.mrh0.createaddition.energy.network.EnergyNetwork;
import com.mrh0.createaddition.network.EnergyNetworkPacket;
import com.mrh0.createaddition.network.IObserveTileEntity;
import com.mrh0.createaddition.network.ObservePacket;
import com.mrh0.createaddition.network.RemoveConnectorPacket;
import com.mrh0.createaddition.util.Util;
import com.quattage.experimental_tables.content.block.LConnectorBlock;
import com.simibubi.create.content.contraptions.goggles.IHaveGoggleInformation;

import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import team.reborn.energy.api.EnergyStorage;



public class LConnectorBlockEntity extends BaseElectricTileEntity implements IWireNode, IObserveTileEntity, IHaveGoggleInformation {
    private final BlockPos[] connectionPos;
	private final int[] connectionIndices;
	private final WireType[] connectionTypes;
	public IWireNode[] nodeCache;

	public static Vec3d OFFSET_DOWN = new Vec3d(0f, -1f/16f, 0f);
	public static Vec3d OFFSET_UP = new Vec3d(0f, 1f/16f, 0f);
	public static Vec3d OFFSET_NORTH = new Vec3d(0f, 0f, -1f/16f);
	public static Vec3d OFFSET_WEST = new Vec3d(-1f/16f, 0f, 0f);
	public static Vec3d OFFSET_SOUTH = new Vec3d(0f, 0f, 1f/16f);
	public static Vec3d OFFSET_EAST = new Vec3d(1f/16f, 0f, 0f);

    public static final int NODE_COUNT = 4;
    public static final long CAPACITY = 512, MAX_IN = 256, MAX_OUT = 256;

    
    public LConnectorBlockEntity(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state) {
		super(tileEntityTypeIn, pos, state, CAPACITY, MAX_IN, MAX_OUT);
		
		connectionPos = new BlockPos[getNodeCount()];
		connectionIndices = new int[getNodeCount()];
		connectionTypes = new WireType[getNodeCount()];
		
		nodeCache = new IWireNode[getNodeCount()];
	}

    public IWireNode getNode(int node) {
		if(getNodeType(node) == null) {
			nodeCache[node] = null;
			return null;
		}
		if(nodeCache[node] == null)
			nodeCache[node] = IWireNode.getWireNode(world, getNodePos(node));
		if(nodeCache[node] == null)
			setNode(node, -1, null, null);
		
		return nodeCache[node];
	}

    @Override
	public Vec3d getNodeOffset(int node) {
		switch(getCachedState().get(LConnectorBlock.FACING)) {
			case DOWN:
				return OFFSET_DOWN;
			case UP:
				return OFFSET_UP;
			case NORTH:
				return OFFSET_NORTH;
			case WEST:
				return OFFSET_WEST;
			case SOUTH:
				return OFFSET_SOUTH;
			case EAST:
				return OFFSET_EAST;
		}
		return OFFSET_DOWN;
	}

    @Override
	public boolean isEnergyInput(Direction side) {
		return getCachedState().get(LConnectorBlock.FACING) == side;
	}

    @Override
	public boolean isEnergyOutput(Direction side) {
		return getCachedState().get(LConnectorBlock.FACING) == side;
	}

    @Override
	public int getNodeCount() {
		return NODE_COUNT;
	}

    @Override
	public int getNodeFromPos(Vec3d vector3d) {
		for(int i = 0; i < getNodeCount(); i++) {
			if(hasConnection(i))
				continue;
			return i;
		}
		return -1;
	}

    @Override
	public BlockPos getNodePos(int node) {
		return connectionPos[node];
	}

    @Override
	public WireType getNodeType(int node) {
		return connectionTypes[node];
	}

    @Override
	public int getOtherNodeIndex(int node) {
		if(connectionPos[node] == null)
			return -1;
		return connectionIndices[node];
	}

	@Override
	public void setNode(int node, int other, BlockPos pos, WireType type) {
		connectionPos[node] = pos; 
		connectionIndices[node] = other;
		connectionTypes[node] = type;
		
		// Invalidate
		if(network != null)
			network.invalidate();
	}

    @Override
	public void read(NbtCompound nbt, boolean clientPacket) {
		super.read(nbt, clientPacket);
		for(int i = 0; i < getNodeCount(); i++)
			if(IWireNode.hasNode(nbt, i))
				readNode(nbt, i);
	}
	
	@Override
	public void write(NbtCompound nbt, boolean clientPacket) {
		super.write(nbt, clientPacket);
		for(int i = 0; i < getNodeCount(); i++) {
			if(getNodeType(i) == null)
				IWireNode.clearNode(nbt, i);
			else //?
				writeNode(nbt, i);
		}
	}

    @Override
	public void removeNode(int other) {
		IWireNode.super.removeNode(other);
		invalidateNodeCache();
		this.markDirty();
		
		// Invalidate
		if(network != null)
			network.invalidate();
	}

	@Override
	public BlockPos getMyPos() {
		return pos;
	}

	public void onBlockRemoved() {
		for(int i = 0; i < getNodeCount(); i++) {
			if(getNodeType(i) == null)
				continue;
			IWireNode node = getNode(i);
			if(node == null)
				continue;
			int other = getOtherNodeIndex(i);
			node.removeNode(other);
			node.invalidateNodeCache();
			RemoveConnectorPacket.send(node.getMyPos(), other, world);
		}
		invalidateNodeCache();
		
		if(network != null)
			network.invalidate();
		markRemoved();
	}

    @Override
	public void invalidateNodeCache() {
		for(int i = 0; i < getNodeCount(); i++)
			nodeCache[i] = null;
	}
	
	@Override
	public void tick() {
		super.tick();
		if(world.isClient())
			return;
		if(awakeNetwork(world)) {
			//EnergyNetwork.buildNetwork(world, this);
			causeBlockUpdate();
		}
		networkTick(network);
	}

	private EnergyNetwork network;

    @Override
	public EnergyNetwork getNetwork(int node) {
		return network;
	}

	@Override
	public void setNetwork(int node, EnergyNetwork network) {
		this.network = network;
	}

    private long demand = 0;
	private void networkTick(EnergyNetwork en) {
		if(world.isClient())
			return;
		Direction d = getCachedState().get(LConnectorBlock.FACING);
		EnergyStorage ies = getCachedEnergy(d);
		if(ies == null)
			return;
		
		long pull = en.pull(demand);
		try(Transaction t = Transaction.openOuter()) {
			ies.insert(pull, t);

			long testExtract, testInsert;
			try(Transaction nested = Transaction.openNested(t)) {
				testExtract = energy.extract(Long.MAX_VALUE, nested);
				testInsert = ies.insert(MAX_OUT, nested);
			}
			demand = en.demand(testInsert);


			long push = en.push(testExtract);
			long ext = energy.internalConsumeEnergy(push);
			t.commit();
		}
	}

    @Override
	public void onObserved(ServerPlayerEntity player, ObservePacket pack) {
		if(isNetworkValid(0))
			EnergyNetworkPacket.send(pos, getNetwork(0).getPulled(), getNetwork(0).getPushed(), player);
	}

    @Override
	public boolean addToGoggleTooltip(List<Text> tooltip, boolean isPlayerSneaking) {
		ObservePacket.send(pos, 0);
		tooltip.add(Text.literal(spacing)
				.append(Text.translatable(CreateAddition.MODID + ".tooltip.connector.info").formatted(Formatting.WHITE)));
		
		tooltip.add(Text.literal(spacing)
				.append(Text.translatable(CreateAddition.MODID + ".tooltip.energy.usage").formatted(Formatting.GRAY)));
		tooltip.add(Text.literal(spacing).append(" ")
				.append(Util.format((int)EnergyNetworkPacket.clientBuff)).append("fe/t").formatted(Formatting.AQUA));
		
		return IHaveGoggleInformation.super.addToGoggleTooltip(tooltip, isPlayerSneaking);
	}
}
