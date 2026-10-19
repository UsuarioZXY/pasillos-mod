package com.pasillos.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.pasillos.PasillosMod;
import com.pasillos.entity.Ciego;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * El Ciego: cuerpo flaco de costillas marcadas con una cresta de espinazo, cuello largo y cabeza sin ojos
 * (con las cuencas cosidas), hocico largo con colmillos, una mandibula que se abre mucho al cazar,
 * patas finas y largas que doblan raro y una cola de rata. Textura de 128x64 (ver gen_round8.py: PARTS).
 *
 * Al no ver, la cabeza barre de un lado a otro olfateando; al cazar baja, se queda fija hacia delante,
 * abre la boca y las patas pasan a un galope.
 */
public class CiegoModel extends EntityModel<Ciego> {

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation(PasillosMod.MODID, "ciego"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart tail;
    private final ModelPart[] leg = new ModelPart[4];   // 0 delantera dcha, 1 delantera izq, 2 trasera dcha, 3 trasera izq
    private final ModelPart[] shin = new ModelPart[4];

    public CiegoModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.neck = this.body.getChild("neck");
        this.head = this.neck.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.tail = this.body.getChild("tail");
        String[] names = {"front_right", "front_left", "hind_right", "hind_left"};
        for (int i = 0; i < 4; i++) {
            this.leg[i] = root.getChild(names[i]);
            this.shin[i] = this.leg[i].getChild(names[i] + "_shin");
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-3.0F, -3.0F, -8.0F, 6.0F, 7.0F, 16.0F)
                        .texOffs(0, 24).addBox(-0.5F, -5.0F, -7.0F, 1.0F, 2.0F, 14.0F),
                PartPose.offset(0.0F, 7.5F, 0.0F));

        PartDefinition neck = body.addOrReplaceChild("neck", CubeListBuilder.create()
                        .texOffs(48, 0).addBox(-1.5F, -1.5F, -9.0F, 3.0F, 3.0F, 9.0F),
                PartPose.offset(0.0F, -1.0F, -7.0F));

        PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(76, 0).addBox(-2.5F, -2.5F, -4.0F, 5.0F, 5.0F, 5.0F)
                        .texOffs(100, 0).addBox(-1.5F, -0.5F, -9.0F, 3.0F, 2.0F, 5.0F)
                        // colmillos de arriba
                        .texOffs(116, 0).addBox(-1.5F, 1.5F, -8.0F, 1.0F, 2.0F, 1.0F)
                        .texOffs(116, 0).addBox(0.5F, 1.5F, -8.0F, 1.0F, 2.0F, 1.0F)
                        .texOffs(116, 0).addBox(-1.5F, 1.5F, -6.0F, 1.0F, 2.0F, 1.0F)
                        .texOffs(116, 0).addBox(0.5F, 1.5F, -6.0F, 1.0F, 2.0F, 1.0F)
                        // colgajos de piel a los lados, donde deberian estar las orejas
                        .texOffs(116, 4).addBox(-3.5F, -2.0F, -1.0F, 1.0F, 3.0F, 3.0F)
                        .texOffs(116, 4).addBox(2.5F, -2.0F, -1.0F, 1.0F, 3.0F, 3.0F),
                PartPose.offset(0.0F, 0.0F, -9.0F));

        // mandibula: bisagra atras, se abre hacia abajo
        head.addOrReplaceChild("jaw", CubeListBuilder.create()
                        .texOffs(100, 8).addBox(-1.5F, 0.0F, -5.0F, 3.0F, 1.0F, 5.0F)
                        .texOffs(116, 0).addBox(-1.5F, -1.0F, -4.5F, 1.0F, 2.0F, 1.0F)
                        .texOffs(116, 0).addBox(0.5F, -1.0F, -4.5F, 1.0F, 2.0F, 1.0F),
                PartPose.offset(0.0F, 1.5F, -4.0F));

        body.addOrReplaceChild("tail", CubeListBuilder.create()
                        .texOffs(48, 14).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 1.0F, 10.0F),
                PartPose.offsetAndRotation(0.0F, -1.5F, 8.0F, 0.8F, 0.0F, 0.0F));

        String[] names = {"front_right", "front_left", "hind_right", "hind_left"};
        for (int i = 0; i < 4; i++) {
            float x = (i % 2 == 0) ? -2.6F : 2.6F;
            float z = i < 2 ? -5.5F : 5.5F;
            PartDefinition upper = root.addOrReplaceChild(names[i], CubeListBuilder.create()
                            .texOffs(0, 44).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F),
                    PartPose.offset(x, 10.5F, z));
            PartDefinition lower = upper.addOrReplaceChild(names[i] + "_shin", CubeListBuilder.create()
                            .texOffs(12, 44).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 7.0F, 2.0F),
                    PartPose.offset(0.0F, 6.0F, 0.0F));
            PartDefinition paw = lower.addOrReplaceChild(names[i] + "_paw", CubeListBuilder.create()
                            .texOffs(24, 44).addBox(-1.5F, 0.0F, -3.0F, 3.0F, 1.0F, 4.0F),
                    PartPose.offset(0.0F, 7.0F, 0.0F));
            // tres garras largas
            for (int c = 0; c < 3; c++) {
                paw.addOrReplaceChild(names[i] + "_claw" + c, CubeListBuilder.create()
                                .texOffs(40, 44).addBox(-1.5F + c, 0.0F, -5.0F, 1.0F, 1.0F, 2.0F),
                        PartPose.ZERO);
            }
        }
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(Ciego entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        float pt = ageInTicks - entity.tickCount;
        float hunt = entity.getHuntAmount(pt);
        float t = ageInTicks + (entity.getId() % 53) * 7.0F;
        float amp = Mth.clamp(limbSwingAmount, 0.0F, 1.0F);
        float sw = limbSwing * (0.6F + 0.45F * hunt);

        // patas: trote diagonal al andar, galope (pares juntos) al cazar
        float front = Mth.cos(sw) * 1.15F * amp;
        float frontOther = Mth.lerp(hunt, Mth.cos(sw + Mth.PI), Mth.cos(sw + 0.35F)) * 1.15F * amp;
        float hind = Mth.lerp(hunt, Mth.cos(sw + Mth.PI), Mth.cos(sw + Mth.PI + 0.35F)) * 1.2F * amp;
        float hindOther = Mth.cos(sw + Mth.lerp(hunt, 0.0F, Mth.PI)) * 1.2F * amp;
        this.leg[0].xRot = front;
        this.leg[1].xRot = frontOther;
        this.leg[2].xRot = hind;
        this.leg[3].xRot = hindOther;
        for (int i = 0; i < 4; i++) {
            float bend = Math.max(0.0F, Mth.sin(sw + (i % 2 == 0 ? 0.0F : Mth.PI) + 0.9F)) * 0.9F * amp + 0.12F;
            this.shin[i].xRot = i < 2 ? bend : -bend * 1.1F;   // las traseras doblan al reves
        }

        // cuerpo
        this.body.y = 7.5F + Mth.sin(t * 0.1F) * 0.3F - hunt * 0.8F;
        this.body.xRot = hunt * 0.12F + Mth.sin(sw * 2.0F) * 0.03F * amp;
        this.body.yRot = Mth.sin(sw) * 0.07F * amp;

        // cuello y cabeza: barre de lado a lado olfateando; al cazar baja y se queda fijo
        this.neck.xRot = Mth.lerp(hunt, -0.55F + Mth.sin(t * 0.05F) * 0.12F, 0.5F);
        this.neck.yRot = Mth.sin(t * 0.09F) * 0.55F * (1.0F - hunt) + Mth.sin(t * 1.3F) * 0.04F * hunt;
        this.head.xRot = Mth.sin(t * 0.13F) * 0.18F * (1.0F - hunt)
                + Math.max(0.0F, Mth.sin(t * 0.9F)) * 0.1F * (1.0F - hunt);
        this.head.zRot = 0.22F + Mth.sin(t * 0.07F) * 0.1F;
        this.jaw.xRot = 0.12F + Math.max(0.0F, Mth.sin(t * 0.4F)) * 0.1F * (1.0F - hunt)
                + hunt * (0.95F + Mth.sin(t * 3.0F) * 0.06F);

        // cola
        this.tail.xRot = 0.8F;
        this.tail.yRot = Mth.sin(t * 0.12F) * 0.3F + Mth.cos(sw) * 0.4F * amp;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
