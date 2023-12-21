package earth.terrarium.adastra.common.blocks;

import earth.terrarium.adastra.common.blocks.properties.LaunchPadPartProperty;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

@SuppressWarnings("deprecation")
public class LaunchPadBlock extends Block implements SimpleWaterloggedBlock {

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final EnumProperty<LaunchPadPartProperty> PART = EnumProperty.create("part", LaunchPadPartProperty.class);

    public static final VoxelShape SHAPE = Shapes.or(Block.box(0, 0, 0, 16, 2, 16));

    public LaunchPadBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
            .setValue(POWERED, false)
            .setValue(PART, LaunchPadPartProperty.CENTER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED, POWERED, PART);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ?
            Fluids.WATER.getSource(false) :
            super.getFluidState(state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return state.getValue(PART).isController() ? RenderShape.MODEL : RenderShape.INVISIBLE;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
        return defaultBlockState().setValue(WATERLOGGED, fluidState.getType().equals(Fluids.WATER));
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.isClientSide()) {
            level.setBlock(pos, state.setValue(POWERED, level.hasNeighborSignal(pos)), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        for (var part : LaunchPadPartProperty.values()) {
            var partPos = pos.north(part.xOffset()).east(part.yOffset());
            level.setBlock(partPos, state.setValue(PART, part), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (!Block.canSupportRigidBlock(level, pos.below())) return false;
        for (var part : LaunchPadPartProperty.values()) {
            var offset = pos.north(part.xOffset()).east(part.yOffset());
            if (!level.getBlockState(offset).isAir()) return false;
        }
        return true;
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        destroy(level, pos, state);
        super.playerWillDestroy(level, pos, state, player);
    }

//    @Override
//    public void wasExploded(Level level, BlockPos pos, Explosion explosion) { //TODO
//        destroy(level, pos, level.getBlockState(pos));
//        super.wasExploded(level, pos, explosion);
//    }

    private void destroy(Level level, BlockPos pos, BlockState state) {
        var controllerPos = getController(state, pos);
        for (var part : LaunchPadPartProperty.values()) {
            var partPos = controllerPos.north(part.xOffset()).east(part.yOffset());
            level.destroyBlock(partPos, false);
        }
    }

    private BlockPos getController(BlockState state, BlockPos pos) {
        var part = state.getValue(PART);
        return pos.south(part.xOffset()).west(part.yOffset());
    }
}
