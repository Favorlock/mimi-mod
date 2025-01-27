package io.github.tofodroid.mods.mimi.common.block;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import io.github.tofodroid.mods.mimi.common.MIMIMod;
import io.github.tofodroid.mods.mimi.common.config.instrument.InstrumentConfig;
import io.github.tofodroid.mods.mimi.common.config.instrument.InstrumentSpec;
import io.github.tofodroid.mods.mimi.util.VoxelShapeUtils;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MIMIMod.MODID);

    // Redstone Blocks
    public static final RegistryObject<BlockListener> LISTENER = BLOCKS.register(BlockListener.REGISTRY_NAME, () -> new BlockListener());
    public static final RegistryObject<BlockReceiver> RECEIVER = BLOCKS.register(BlockReceiver.REGISTRY_NAME, () -> new BlockReceiver());
    public static final RegistryObject<BlockMechanicalMaestro> MECHANICALMAESTRO = BLOCKS.register(BlockMechanicalMaestro.REGISTRY_NAME, () -> new BlockMechanicalMaestro());
    public static final RegistryObject<BlockConductor> CONDUCTOR = BLOCKS.register(BlockConductor.REGISTRY_NAME, () -> new BlockConductor());

    // Village Blocks
    public static final RegistryObject<BlockTuningTable> TUNINGTABLE = BLOCKS.register(BlockTuningTable.REGISTRY_NAME, () -> new BlockTuningTable());

    // Other Blocks
    public static final RegistryObject<BlockDiskWriter> DISKWRITER = BLOCKS.register(BlockDiskWriter.REGISTRY_NAME, () -> new BlockDiskWriter());
    public static final RegistryObject<BlockBroadcaster> BROADCASTER = BLOCKS.register(BlockBroadcaster.REGISTRY_NAME, () -> new BlockBroadcaster());

    // Instrument Blocks
    protected static List<RegistryObject<BlockInstrument>> INSTRUMENTS = buildInstruments();

    public static List<BlockInstrument> getBlockInstruments() {
        return INSTRUMENTS.stream()
            .map(i -> i.get())
            .collect(Collectors.toList());
    }

    public static List<RegistryObject<BlockInstrument>> buildInstruments()  {
        List<RegistryObject<BlockInstrument>> result = new ArrayList<>();
        for(InstrumentSpec instrument : InstrumentConfig.getBlockInstruments()) {
            result.add(
                BLOCKS.register(
                    instrument.registryName, 
                    () -> new BlockInstrument(instrument.instrumentId, instrument.registryName, instrument.isDyeable(), instrument.defaultColor(), VoxelShapeUtils.loadFromStrings(instrument.collisionShapes))
                )
            );
        }
        return result;
    }
}
