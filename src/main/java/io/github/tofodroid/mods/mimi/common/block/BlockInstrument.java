package io.github.tofodroid.mods.mimi.common.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.IWaterLoggable;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import java.util.Map;
import java.util.UUID;

import io.github.tofodroid.mods.mimi.common.MIMIMod;
import io.github.tofodroid.mods.mimi.common.entity.EntitySeat;
import io.github.tofodroid.mods.mimi.common.entity.ModEntities;
import io.github.tofodroid.mods.mimi.common.instruments.InstrumentDataUtil;
import io.github.tofodroid.mods.mimi.common.instruments.ItemInstrumentDataUtil;
import io.github.tofodroid.mods.mimi.common.item.ItemInstrument;
import io.github.tofodroid.mods.mimi.common.tile.ModTiles;
import io.github.tofodroid.mods.mimi.common.tile.TileInstrument;
import io.github.tofodroid.mods.mimi.util.PlayerNameUtils;

public abstract class BlockInstrument extends Block implements IWaterLoggable {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final DirectionProperty DIRECTION = BlockStateProperties.HORIZONTAL_FACING;

    public final Map<Direction, VoxelShape> SHAPES;
    private final Byte instrumentId;

    public BlockInstrument(Properties properties, Byte instrumentId) {
        super(properties);
        this.instrumentId = instrumentId;
        this.setDefaultState(this.getStateContainer().getBaseState()
            .with(WATERLOGGED, false)
            .with(DIRECTION, Direction.NORTH)
        );
        this.SHAPES = this.generateShapes();
    }

    protected abstract Map<Direction, VoxelShape> generateShapes();

    @Override
    public ActionResultType onBlockActivated(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
        TileInstrument tileInstrument = getTileInstrumentForBlock(worldIn, pos);
        

        if(tileInstrument != null) {
            UUID instrumentMaestro = ItemInstrumentDataUtil.INSTANCE.getLinkedMaestro(ItemInstrument.getEntityHeldInstrumentStack(player, hand));

            // Server-Side: If right clicked with instrument and not currently being used then set maestro, otherwise sit
            if(instrumentMaestro != null && !InstrumentDataUtil.MIDI_MAESTRO_ID.equals(instrumentMaestro) && !InstrumentDataUtil.PUBLIC_MAESTRO_ID.equals(instrumentMaestro) && !EntitySeat.seatExists(worldIn, pos, this.getSeatOffset(state))) {
                if(!worldIn.isRemote) {
                    tileInstrument.setMaestro(instrumentMaestro);
                    tileInstrument.markDirty();
                    ((ServerWorld)worldIn).getChunkProvider().markBlockChanged(pos);
                } else {
                    String instrumentMaestroName = PlayerNameUtils.getPlayerNameFromUUID(instrumentMaestro, worldIn);
                    player.sendStatusMessage(new StringTextComponent("Linked " + this.getTranslatedName().getString() + " Maestro: " +  instrumentMaestroName), true);
                }
            } else if(instrumentMaestro != null && worldIn.isRemote) {
                player.sendStatusMessage(new StringTextComponent("You can only set the Maestro when the instrument is not being used."), true);
            } else if(!worldIn.isRemote) {
                return EntitySeat.create(worldIn, pos, this.getSeatOffset(state), player);
            } else if(worldIn.isRemote) {
                if(tileInstrument.equals(getTileInstrumentForEntity(player))) {
                    MIMIMod.guiWrapper.openInstrumentGui(worldIn, player, instrumentId, tileInstrument);
                }
            }
        }

        return ActionResultType.SUCCESS;
    }

    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        FluidState fluidState = context.getWorld().getFluidState(context.getPos());
        return this.getDefaultState()
            .with(WATERLOGGED, fluidState.getFluid() == Fluids.WATER)
            .with(DIRECTION, context.getPlacementHorizontalFacing().getOpposite());
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        super.fillStateContainer(builder);
        builder.add(DIRECTION);
        builder.add(WATERLOGGED);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.with(DIRECTION, rotation.rotate(state.get(DIRECTION)));
    }

    @Override
    @SuppressWarnings("deprecation")
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStillFluidState(false) : super.getFluidState(state);
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.toRotation(state.get(DIRECTION)));
    }
    
    @Override
    public boolean hasTileEntity(final BlockState state) {
        return true;
    }
    
    @Override
	public TileEntity createTileEntity(final BlockState state, final IBlockReader reader) {
		TileInstrument tile = ModTiles.INSTRUMENT.create();
        tile.setInstrumentId(instrumentId);
        return tile;
	}

    public Byte getInstrumentId() {
        return instrumentId;
    }

    public static TileInstrument getTileInstrumentForBlock(World worldIn, BlockPos pos) {
        TileEntity entity = worldIn.getTileEntity(pos);
        return entity != null && entity instanceof TileInstrument ? (TileInstrument)entity : null;
    }

    protected Vector3d getSeatOffset(BlockState state) {
        switch(state.get(DIRECTION)) {
            case NORTH:
                return new Vector3d(0.5, 0, 0.05);
            case SOUTH:
                return new Vector3d(0.5, 0, 0.95);
            case EAST:
                return new Vector3d(0.95, 0, 0.5);
            case WEST:
                return new Vector3d(0.05, 0, 0.5);
            default:
                return new Vector3d(0.5, 0, 0.05);
        }
    }

    public static Boolean isEntitySittingAtInstrument(LivingEntity entity) {
        return entity.isPassenger() && ModEntities.SEAT.equals(entity.getRidingEntity().getType());
    }
    
    public static EntitySeat getSeatForEntity(LivingEntity entity) {
        if(isEntitySittingAtInstrument(entity)) {
            return (EntitySeat) entity.getRidingEntity();
        }

        return null;
    }
    
    public static TileInstrument getTileInstrumentForEntity(LivingEntity entity) {
        if(entity.isAlive() && isEntitySittingAtInstrument(entity)) {
            BlockPos pos = getSeatForEntity(entity).getSource();

            if(pos != null && !World.isOutsideBuildHeight(pos)) {
                TileEntity sourceEntity = entity.getEntityWorld().getTileEntity(getSeatForEntity(entity).getSource());
                return sourceEntity != null && sourceEntity instanceof TileInstrument ? (TileInstrument) sourceEntity : null;
            }
        }

        return null;
    }
}
