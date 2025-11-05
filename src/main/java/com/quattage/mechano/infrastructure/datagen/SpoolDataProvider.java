package com.quattage.mechano.infrastructure.datagen;

public class SpoolDataProvider {

    // public static <T extends SpoolItem<?>> void generate(DataGenContext<Item, T> ctx, RegistrateItemModelProvider prov) {
    //     ResourceLocation loc = ctx.getId();
    //     ModelFile generated = new ModelFile.UncheckedModelFile("item/generated");
    //     ResourceLocation full = ResourceLocation.fromNamespaceAndPath(loc.getNamespace(), loc.getPath() + "_full");
    //     ResourceLocation depleted = ResourceLocation.fromNamespaceAndPath(loc.getNamespace(), loc.getPath() + "_depleted");
    //     ResourceLocation empty = ResourceLocation.fromNamespaceAndPath(loc.getNamespace(), loc.getPath() + "_empty");
    //     ResourceLocation totallyEmpty = ResourceLocation.fromNamespaceAndPath(loc.getNamespace(), "spool_empty");
    //     prov.getBuilder(full.toString()).parent(generated).texture("layer0", ResourceLocation.fromNamespaceAndPath(loc.getNamespace(), "item/" + loc.getPath()));
    //     prov.basicItem(depleted);
    //     prov.basicItem(empty);
    //     prov.getBuilder(loc.toString()).parent(generated)
    //         .override().predicate(MechanoItemProperties.FULLNESS, 0.00f).model(prov.getExistingFile(totallyEmpty)).end()
    //         .override().predicate(MechanoItemProperties.FULLNESS, 0.10f).model(prov.getExistingFile(empty)).end()
    //         .override().predicate(MechanoItemProperties.FULLNESS, 0.50f).model(prov.getExistingFile(depleted)).end()
    //         .override().predicate(MechanoItemProperties.FULLNESS, 0.75f).model(prov.getExistingFile(full)).end()
    //         .override().predicate(MechanoItemProperties.FULLNESS, 0.999f).model(prov.getExistingFile(full)).end();
    // }
}
