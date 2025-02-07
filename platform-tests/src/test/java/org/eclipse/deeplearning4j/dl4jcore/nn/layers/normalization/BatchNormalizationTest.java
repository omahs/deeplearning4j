/*
 *  ******************************************************************************
 *  *
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Apache License, Version 2.0 which is available at
 *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *
 *  *  See the NOTICE file distributed with this work for additional
 *  *  information regarding copyright ownership.
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  * License for the specific language governing permissions and limitations
 *  * under the License.
 *  *
 *  * SPDX-License-Identifier: Apache-2.0
 *  *****************************************************************************
 */
package org.eclipse.deeplearning4j.dl4jcore.nn.layers.normalization;

import lombok.extern.slf4j.Slf4j;
import org.deeplearning4j.BaseDL4JTest;
import org.eclipse.deeplearning4j.dl4jcore.TestUtils;
import org.deeplearning4j.datasets.iterator.EarlyTerminationDataSetIterator;
import org.deeplearning4j.datasets.iterator.utilty.ListDataSetIterator;
import org.deeplearning4j.datasets.iterator.impl.MnistDataSetIterator;
import org.deeplearning4j.nn.api.Layer;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.ConvolutionMode;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.*;
import org.deeplearning4j.nn.conf.layers.BatchNormalization;
import org.deeplearning4j.nn.gradient.Gradient;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.params.BatchNormalizationParamInitializer;
import org.deeplearning4j.nn.transferlearning.FineTuneConfiguration;
import org.deeplearning4j.nn.transferlearning.TransferLearning;
import org.deeplearning4j.nn.updater.MultiLayerUpdater;
import org.deeplearning4j.nn.updater.UpdaterBlock;
import org.deeplearning4j.nn.weights.WeightInit;
import org.junit.jupiter.api.*;
import org.nd4j.common.tests.tags.NativeTag;
import org.nd4j.common.tests.tags.TagNames;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.buffer.util.DataTypeUtil;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.impl.broadcast.BroadcastAddOp;
import org.nd4j.linalg.api.ops.impl.broadcast.BroadcastDivOp;
import org.nd4j.linalg.api.ops.impl.broadcast.BroadcastMulOp;
import org.nd4j.linalg.api.ops.impl.broadcast.BroadcastSubOp;
import org.nd4j.linalg.api.shape.Shape;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.NoOpUpdater;
import org.nd4j.linalg.learning.RmsPropUpdater;
import org.nd4j.linalg.learning.config.AdaDelta;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.nd4j.linalg.ops.transforms.Transforms;
import org.nd4j.common.primitives.Pair;
import org.deeplearning4j.nn.workspace.LayerWorkspaceMgr;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/**
 */
@Slf4j
@DisplayName("Batch Normalization Test")
@NativeTag
@Tag(TagNames.DL4J_OLD_API)
@Tag(TagNames.LONG_TEST)
@Tag(TagNames.LARGE_RESOURCES)
class BatchNormalizationTest extends BaseDL4JTest {

    static {
        // Force Nd4j initialization, then set data type to double:
        Nd4j.zeros(1);
        DataTypeUtil.setDTypeForContext(DataType.DOUBLE);
    }

    protected INDArray dnnInput = Nd4j.linspace(0, 31, 32, Nd4j.dataType()).reshape(2, 16);

    protected INDArray dnnEpsilon = Nd4j.linspace(0, 31, 32, Nd4j.dataType()).reshape(2, 16);

    protected INDArray cnnInput = Nd4j.linspace(0, 63, 64, Nd4j.dataType()).reshape(2, 2, 4, 4);

    protected INDArray cnnEpsilon = Nd4j.linspace(0, 63, 64, Nd4j.dataType()).reshape(2, 2, 4, 4);

    @BeforeEach
    void doBefore() {
    }

    @Override
    public long getTimeoutMilliseconds() {
        return 90000L;
    }

    @Test
    @DisplayName("Test Dnn Forward Pass")
    void testDnnForwardPass() {
        int nOut = 10;
        Layer l = getLayer(nOut, 0.0, false, -1, -1);
        // Gamma, beta, global mean, global var
        assertEquals(4 * nOut, l.numParams());
        INDArray randInput = Nd4j.rand(100, nOut);
        INDArray output = l.activate(randInput, true, LayerWorkspaceMgr.noWorkspaces());
        INDArray mean = output.mean(0);
        INDArray stdev = output.std(false, 0);
        assertArrayEquals(new float[nOut], mean.data().asFloat(), 1e-6f);
        assertEquals(Nd4j.ones(nOut), stdev);
        // If we fix gamma/beta: expect different mean and variance...
        double gamma = 2.0;
        double beta = 3.0;
        l = getLayer(nOut, 0.0, true, gamma, beta);
        // Should have only global mean/var parameters
        assertEquals(2 * nOut, l.numParams());
        output = l.activate(randInput, true, LayerWorkspaceMgr.noWorkspaces());
        mean = output.mean(0);
        stdev = output.std(false, 0);
        assertEquals(Nd4j.valueArrayOf(mean.shape(), beta), mean);
        assertEquals(Nd4j.valueArrayOf(stdev.shape(), gamma), stdev);
    }

    protected static Layer getLayer(int nOut, double epsilon, boolean lockGammaBeta, double gamma, double beta) {
        BatchNormalization.Builder b = new BatchNormalization.Builder().nOut(nOut).eps(epsilon);
        if (lockGammaBeta) {
            b.lockGammaBeta(true).gamma(gamma).beta(beta);
        }
        BatchNormalization bN = b.build();
        NeuralNetConfiguration conf = new NeuralNetConfiguration.Builder().layer(bN).build();
        long numParams = conf.getLayer().initializer().numParams(conf);
        INDArray params = null;
        if (numParams > 0) {
            params = Nd4j.create(1, numParams);
        }
        Layer layer = conf.getLayer().instantiate(conf, null, 0, params, true, params == null ? Nd4j.defaultFloatingPointType() : params.dataType());
        if (numParams > 0) {
            layer.setBackpropGradientsViewArray(Nd4j.create(1, numParams));
        }
        return layer;
    }

    @Test
    @DisplayName("Test Dnn Forward Backward")
    void testDnnForwardBackward() {
        double eps = 1e-5;
        int nIn = 4;
        int minibatch = 2;
        Nd4j.getRandom().setSeed(12345);
        INDArray input = Nd4j.rand('c', new int[] { minibatch, nIn });
        // TODO: other values for gamma/beta
        INDArray gamma = Nd4j.ones(1, nIn);
        INDArray beta = Nd4j.zeros(1, nIn);
        Layer l = getLayer(nIn, eps, false, -1, -1);
        INDArray mean = input.mean(0);
        INDArray var = input.var(false, 0);
        INDArray xHat = input.subRowVector(mean).divRowVector(Transforms.sqrt(var.add(eps), true));
        INDArray outExpected = xHat.mulRowVector(gamma).addRowVector(beta);
        INDArray out = l.activate(input, true, LayerWorkspaceMgr.noWorkspaces());
        assertEquals(outExpected, out);
        // -------------------------------------------------------------
        // Check backprop
        // dL/dy
        INDArray epsilon = Nd4j.rand(minibatch, nIn);
        INDArray dldgammaExp = epsilon.mul(xHat).sum(true, 0);
        INDArray dldbetaExp = epsilon.sum(true, 0);
        INDArray dldxhat = epsilon.mulRowVector(gamma);
        INDArray dldvar = dldxhat.mul(input.subRowVector(mean)).mul(-0.5).mulRowVector(Transforms.pow(var.add(eps), -3.0 / 2.0, true)).sum(0);
        INDArray dldmu = dldxhat.mulRowVector(Transforms.pow(var.add(eps), -1.0 / 2.0, true)).neg().sum(0).add(dldvar.mul(input.subRowVector(mean).mul(-2.0).sum(0).div(minibatch)));
        INDArray dldinExp = dldxhat.mulRowVector(Transforms.pow(var.add(eps), -1.0 / 2.0, true)).add(input.subRowVector(mean).mul(2.0 / minibatch).mulRowVector(dldvar)).addRowVector(dldmu.mul(1.0 / minibatch));
        Pair<Gradient, INDArray> p = l.backpropGradient(epsilon, LayerWorkspaceMgr.noWorkspaces());
        INDArray dldgamma = p.getFirst().getGradientFor("gamma");
        INDArray dldbeta = p.getFirst().getGradientFor("beta");
        assertEquals(dldgammaExp, dldgamma);
        assertEquals(dldbetaExp, dldbeta);
        assertEquals(dldinExp, p.getSecond());
    }

    @Test
    @DisplayName("Test Cnn Forward Pass")
    void testCnnForwardPass() {
        int nOut = 10;
        Layer l = getLayer(nOut, 0.0, false, -1, -1);
        // Gamma, beta, global mean, global var
        assertEquals(4 * nOut, l.numParams());
        int hw = 15;
        Nd4j.getRandom().setSeed(12345);
        INDArray randInput = Nd4j.rand(new int[] { 100, nOut, hw, hw });
        INDArray output = l.activate(randInput, true, LayerWorkspaceMgr.noWorkspaces());
        assertEquals(4, output.rank());
        INDArray mean = output.mean(0, 2, 3);
        INDArray stdev = output.std(false, 0, 2, 3);
        assertArrayEquals(new float[nOut], mean.data().asFloat(), 1e-6f);
        assertArrayEquals(Nd4j.ones(1, nOut).data().asFloat(), stdev.data().asFloat(), 1e-6f);
        // If we fix gamma/beta: expect different mean and variance...
        double gamma = 2.0;
        double beta = 3.0;
        l = getLayer(nOut, 0.0, true, gamma, beta);
        // Should have only global mean/var parameters
        assertEquals(2 * nOut, l.numParams());
        output = l.activate(randInput, true, LayerWorkspaceMgr.noWorkspaces());
        mean = output.mean(0, 2, 3);
        stdev = output.std(false, 0, 2, 3);
        assertEquals(Nd4j.valueArrayOf(mean.shape(), beta), mean);
        assertEquals(Nd4j.valueArrayOf(stdev.shape(), gamma), stdev);
    }

    @Test
    @DisplayName("Test 2 d Vs 4 d")
    void test2dVs4d() {
        // Idea: 2d and 4d should be the same...
        Nd4j.getRandom().setSeed(12345);
        int m = 2;
        int h = 3;
        int w = 3;
        int nOut = 2;
        INDArray in = Nd4j.rand('c', m * h * w, nOut);
        INDArray in4 = in.dup();
        in4 = Shape.newShapeNoCopy(in4, new int[] { m, h, w, nOut }, false);
        assertNotNull(in4);
        in4 = in4.permute(0, 3, 1, 2).dup();
        INDArray arr = Nd4j.rand(1, m * h * w * nOut).reshape('f', h, w, m, nOut).permute(2, 3, 1, 0);
        in4 = arr.assign(in4);
        Layer l1 = getLayer(nOut);
        Layer l2 = getLayer(nOut);
        INDArray out2d = l1.activate(in.dup(), true, LayerWorkspaceMgr.noWorkspaces());
        INDArray out4d = l2.activate(in4.dup(), true, LayerWorkspaceMgr.noWorkspaces());
        INDArray out4dAs2 = out4d.permute(0, 2, 3, 1).dup('c');
        out4dAs2 = Shape.newShapeNoCopy(out4dAs2, new int[] { m * h * w, nOut }, false);
        assertEquals(out2d, out4dAs2);
        // Test backprop:
        INDArray epsilons2d = Nd4j.rand('c', m * h * w, nOut);
        INDArray epsilons4d = epsilons2d.dup();
        epsilons4d = Shape.newShapeNoCopy(epsilons4d, new int[] { m, h, w, nOut }, false);
        assertNotNull(epsilons4d);
        epsilons4d = epsilons4d.permute(0, 3, 1, 2).dup();
        Pair<Gradient, INDArray> b2d = l1.backpropGradient(epsilons2d, LayerWorkspaceMgr.noWorkspaces());
        Pair<Gradient, INDArray> b4d = l2.backpropGradient(epsilons4d, LayerWorkspaceMgr.noWorkspaces());
        INDArray e4dAs2d = b4d.getSecond().permute(0, 2, 3, 1).dup('c');
        e4dAs2d = Shape.newShapeNoCopy(e4dAs2d, new int[] { m * h * w, nOut }, false);
        assertEquals(b2d.getSecond(), e4dAs2d);
    }

    protected static Layer getLayer(int nOut) {
        return getLayer(nOut, Nd4j.EPS_THRESHOLD, false, -1, -1);
    }

    @Test
    @DisplayName("Test Cnn Forward Backward")
    void testCnnForwardBackward() {
        double eps = 1e-5;
        int nIn = 4;
        int hw = 3;
        int minibatch = 2;
        Nd4j.getRandom().setSeed(12345);
        INDArray input = Nd4j.rand('c', new int[] { minibatch, nIn, hw, hw });
        // TODO: other values for gamma/beta
        INDArray gamma = Nd4j.ones(1, nIn);
        INDArray beta = Nd4j.zeros(1, nIn);
        Layer l = getLayer(nIn, eps, false, -1, -1);
        INDArray mean = input.mean(0, 2, 3);
        INDArray var = input.var(false, 0, 2, 3);
        INDArray xHat = Nd4j.getExecutioner().exec(new BroadcastSubOp(input, mean, input.dup(), 1));
        Nd4j.getExecutioner().exec(new BroadcastDivOp(xHat, Transforms.sqrt(var.add(eps), true), xHat, 1));
        INDArray outExpected = Nd4j.getExecutioner().exec(new BroadcastMulOp(xHat, gamma, xHat.dup(), 1));
        Nd4j.getExecutioner().exec(new BroadcastAddOp(outExpected, beta, outExpected, 1));
        INDArray out = l.activate(input, true, LayerWorkspaceMgr.noWorkspaces());

        assertEquals(outExpected, out);
        // -------------------------------------------------------------
        // Check backprop
        // dL/dy
        INDArray epsilon = Nd4j.rand('c', new int[] { minibatch, nIn, hw, hw });
        int effectiveMinibatch = minibatch * hw * hw;
        INDArray dldgammaExp = epsilon.mul(xHat).sum(0, 2, 3);
        dldgammaExp = dldgammaExp.reshape(1, dldgammaExp.length());
        INDArray dldbetaExp = epsilon.sum(0, 2, 3);
        dldbetaExp = dldbetaExp.reshape(1, dldbetaExp.length());
        // epsilon.mulRowVector(gamma);
        INDArray dldxhat = Nd4j.getExecutioner().exec(new BroadcastMulOp(epsilon, gamma, epsilon.dup(), 1));
        INDArray inputSubMean = Nd4j.getExecutioner().exec(new BroadcastSubOp(input, mean, input.dup(), 1));
        INDArray dldvar = dldxhat.mul(inputSubMean).mul(-0.5);
        dldvar = Nd4j.getExecutioner().exec(new BroadcastMulOp(dldvar, Transforms.pow(var.add(eps), -3.0 / 2.0, true), dldvar.dup(), 1));
        dldvar = dldvar.sum(0, 2, 3);
        INDArray dldmu = Nd4j.getExecutioner().exec(new BroadcastMulOp(dldxhat, Transforms.pow(var.add(eps), -1.0 / 2.0, true), dldxhat.dup(), 1)).neg().sum(0, 2, 3);
        dldmu = dldmu.add(dldvar.mul(inputSubMean.mul(-2.0).sum(0, 2, 3).div(effectiveMinibatch)));
        INDArray dldinExp = Nd4j.getExecutioner().exec(new BroadcastMulOp(dldxhat, Transforms.pow(var.add(eps), -1.0 / 2.0, true), dldxhat.dup(), 1));
        dldinExp = dldinExp.add(Nd4j.getExecutioner().exec(new BroadcastMulOp(inputSubMean.mul(2.0 / effectiveMinibatch), dldvar, inputSubMean.dup(), 1)));
        dldinExp = Nd4j.getExecutioner().exec(new BroadcastAddOp(dldinExp, dldmu.mul(1.0 / effectiveMinibatch), dldinExp.dup(), 1));
        Pair<Gradient, INDArray> p = l.backpropGradient(epsilon, LayerWorkspaceMgr.noWorkspaces());
        INDArray dldgamma = p.getFirst().getGradientFor("gamma");
        INDArray dldbeta = p.getFirst().getGradientFor("beta");
        assertEquals(dldgammaExp, dldgamma);
        assertEquals(dldbetaExp, dldbeta);
        // System.out.println("EPSILONS");
        // System.out.println(Arrays.toString(dldinExp.data().asDouble()));
        // System.out.println(Arrays.toString(p.getSecond().dup().data().asDouble()));
        assertEquals(dldinExp, p.getSecond());
    }

    @Test
    @DisplayName("Test DBNBN Multi Layer")
    void testDBNBNMultiLayer() throws Exception {
        DataSetIterator iter = new MnistDataSetIterator(2, 2);
        DataSet next = iter.next();
        // Run with separate activation layer
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder().optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT).seed(123).list().layer(0, new DenseLayer.Builder().nIn(28 * 28).nOut(10).weightInit(WeightInit.XAVIER).activation(Activation.RELU).build()).layer(1, new BatchNormalization.Builder().nOut(10).build()).layer(2, new ActivationLayer.Builder().activation(Activation.RELU).build()).layer(3, new OutputLayer.Builder(LossFunctions.LossFunction.MCXENT).weightInit(WeightInit.XAVIER).activation(Activation.SOFTMAX).nIn(10).nOut(10).build()).build();
        MultiLayerNetwork network = new MultiLayerNetwork(conf);
        network.init();
        network.setInput(next.getFeatures());
        INDArray activationsActual = network.output(next.getFeatures());
        assertEquals(10, activationsActual.shape()[1], 1e-2);
        network.fit(next);
        INDArray actualGammaParam = network.getLayer(1).getParam(BatchNormalizationParamInitializer.GAMMA);
        INDArray actualBetaParam = network.getLayer(1).getParam(BatchNormalizationParamInitializer.BETA);
        assertTrue(actualGammaParam != null);
        assertTrue(actualBetaParam != null);
    }

    @Test
    @DisplayName("Test CNNBN Activation Combo")
    void testCNNBNActivationCombo() throws Exception {
        DataSetIterator iter = new MnistDataSetIterator(2, 2);
        DataSet next = iter.next();
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder().optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT).seed(123).list().layer(0, new ConvolutionLayer.Builder().nIn(1).nOut(6).weightInit(WeightInit.XAVIER).activation(Activation.IDENTITY).build()).layer(1, new BatchNormalization.Builder().build()).layer(2, new ActivationLayer.Builder().activation(Activation.RELU).build()).layer(3, new OutputLayer.Builder(LossFunctions.LossFunction.MCXENT).weightInit(WeightInit.XAVIER).activation(Activation.SOFTMAX).nOut(10).build()).setInputType(InputType.convolutionalFlat(28, 28, 1)).build();
        MultiLayerNetwork network = new MultiLayerNetwork(conf);
        network.init();
        network.fit(next);
        assertNotEquals(null, network.getLayer(0).getParam("W"));
        assertNotEquals(null, network.getLayer(0).getParam("b"));
    }

    @Test
    @DisplayName("Check Serialization")
    void checkSerialization() throws Exception {
        // Serialize the batch norm network (after training), and make sure we get same activations out as before
        // i.e., make sure state is properly stored
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder().seed(12345).list().layer(0, new ConvolutionLayer.Builder().nIn(1).nOut(6).weightInit(WeightInit.XAVIER).activation(Activation.IDENTITY).build()).layer(1, new BatchNormalization.Builder().build()).layer(2, new ActivationLayer.Builder().activation(Activation.LEAKYRELU).build()).layer(3, new DenseLayer.Builder().nOut(10).activation(Activation.LEAKYRELU).build()).layer(4, new BatchNormalization.Builder().build()).layer(5, new OutputLayer.Builder(LossFunctions.LossFunction.MCXENT).weightInit(WeightInit.XAVIER).activation(Activation.SOFTMAX).nOut(10).build()).setInputType(InputType.convolutionalFlat(28, 28, 1)).build();
        MultiLayerNetwork net = new MultiLayerNetwork(conf);
        net.init();
        DataSetIterator iter = new MnistDataSetIterator(16, true, 12345);
        for (int i = 0; i < 20; i++) {
            net.fit(iter.next());
        }
        INDArray in = iter.next().getFeatures();
        INDArray out = net.output(in, false);
        INDArray out2 = net.output(in, false);
        assertEquals(out, out2);
        MultiLayerNetwork net2 = TestUtils.testModelSerialization(net);
        INDArray outDeser = net2.output(in, false);
        assertEquals(out, outDeser);
    }

    @Test
    @DisplayName("Test Gradient And Updaters")
    void testGradientAndUpdaters() throws Exception {
        // Global mean/variance are part of the parameter vector. Expect 0 gradient, and no-op updater for these
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder().optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT).updater(Updater.RMSPROP).seed(12345).list().layer(0, new ConvolutionLayer.Builder().nIn(1).nOut(6).weightInit(WeightInit.XAVIER).activation(Activation.IDENTITY).build()).layer(1, new BatchNormalization.Builder().build()).layer(2, new ActivationLayer.Builder().activation(Activation.LEAKYRELU).build()).layer(3, new DenseLayer.Builder().nOut(10).activation(Activation.LEAKYRELU).build()).layer(4, new BatchNormalization.Builder().build()).layer(5, new OutputLayer.Builder(LossFunctions.LossFunction.MCXENT).weightInit(WeightInit.XAVIER).activation(Activation.SOFTMAX).nOut(10).build()).setInputType(InputType.convolutionalFlat(28, 28, 1)).build();
        MultiLayerNetwork net = new MultiLayerNetwork(conf);
        net.init();
        DataSetIterator iter = new MnistDataSetIterator(16, true, 12345);
        DataSet ds = iter.next();
        net.setInput(ds.getFeatures());
        net.setLabels(ds.getLabels());
        net.computeGradientAndScore();
        Gradient g = net.gradient();
        Map<String, INDArray> map = g.gradientForVariable();
        org.deeplearning4j.nn.api.Updater u = net.getUpdater();
        MultiLayerUpdater mlu = (MultiLayerUpdater) u;
        List<UpdaterBlock> l = mlu.getUpdaterBlocks();
        assertNotNull(l);
        // Conv+bn (RMSProp), No-op (bn), RMSProp (dense, bn), no-op (bn), RMSProp (out)
        assertEquals(5, l.size());
        for (UpdaterBlock ub : l) {
            List<UpdaterBlock.ParamState> list = ub.getLayersAndVariablesInBlock();
            for (UpdaterBlock.ParamState v : list) {
                if (BatchNormalizationParamInitializer.GLOBAL_MEAN.equals(v.getParamName()) || BatchNormalizationParamInitializer.GLOBAL_VAR.equals(v.getParamName()) || BatchNormalizationParamInitializer.GLOBAL_LOG_STD.equals(v.getParamName())) {
                    assertTrue(ub.getGradientUpdater() instanceof NoOpUpdater);
                } else {
                    assertTrue(ub.getGradientUpdater() instanceof RmsPropUpdater);
                }
            }
        }
    }

    @Test
    @DisplayName("Check Mean Variance Estimate")
    void checkMeanVarianceEstimate() throws Exception {
        Nd4j.getRandom().setSeed(12345);
        // Check that the internal global mean/variance estimate is approximately correct
        for (boolean useLogStd : new boolean[] { true, false }) {
            // First, Mnist data as 2d input (NOT taking into account convolution property)
            MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder().optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT).updater(Updater.RMSPROP).seed(12345).list().layer(0, new BatchNormalization.Builder().nIn(10).nOut(10).eps(1e-5).decay(0.95).useLogStd(useLogStd).build()).layer(1, new OutputLayer.Builder(LossFunctions.LossFunction.MSE).weightInit(WeightInit.XAVIER).activation(Activation.IDENTITY).nIn(10).nOut(10).build()).build();
            MultiLayerNetwork net = new MultiLayerNetwork(conf);
            net.init();
            int minibatch = 32;
            List<DataSet> list = new ArrayList<>();
            for (int i = 0; i < 200; i++) {
                list.add(new DataSet(Nd4j.rand(minibatch, 10), Nd4j.rand(minibatch, 10)));
            }
            DataSetIterator iter = new ListDataSetIterator(list);
            INDArray expMean = Nd4j.valueArrayOf(new int[] { 1, 10 }, 0.5);
            // Expected variance of U(0,1) distribution: 1/12 * (1-0)^2 = 0.0833
            INDArray expVar = Nd4j.valueArrayOf(new int[] { 1, 10 }, 1 / 12.0);
            for (int i = 0; i < 10; i++) {
                iter.reset();
                net.fit(iter);
            }
            INDArray estMean = net.getLayer(0).getParam(BatchNormalizationParamInitializer.GLOBAL_MEAN);
            INDArray estVar;
            if (useLogStd) {
                INDArray log10std = net.getLayer(0).getParam(BatchNormalizationParamInitializer.GLOBAL_LOG_STD);
                estVar = Nd4j.valueArrayOf(log10std.shape(), 10.0).castTo(log10std.dataType());
                // stdev = 10^(log10(stdev))
                Transforms.pow(estVar, log10std, false);
                estVar.muli(estVar);
            } else {
                estVar = net.getLayer(0).getParam(BatchNormalizationParamInitializer.GLOBAL_VAR);
            }
            float[] fMeanExp = expMean.data().asFloat();
            float[] fMeanAct = estMean.data().asFloat();
            float[] fVarExp = expVar.data().asFloat();
            float[] fVarAct = estVar.data().asFloat();
            // System.out.println("Mean vs. estimated mean:");
            // System.out.println(Arrays.toString(fMeanExp));
            // System.out.println(Arrays.toString(fMeanAct));
            // 
            // System.out.println("Var vs. estimated var:");
            // System.out.println(Arrays.toString(fVarExp));
            // System.out.println(Arrays.toString(fVarAct));
            assertArrayEquals(fMeanExp, fMeanAct, 0.02f);
            assertArrayEquals(fVarExp, fVarAct, 0.02f);
        }
    }

    @Test
    @DisplayName("Check Mean Variance Estimate CNN")
    void checkMeanVarianceEstimateCNN() throws Exception {
        for (boolean useLogStd : new boolean[] { true, false }) {
            Nd4j.getRandom().setSeed(12345);
            // Check that the internal global mean/variance estimate is approximately correct
            // First, Mnist data as 2d input (NOT taking into account convolution property)
            MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder().optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT).updater(Updater.RMSPROP).seed(12345).list().layer(0, new BatchNormalization.Builder().nIn(3).nOut(3).eps(1e-5).decay(0.95).useLogStd(useLogStd).build()).layer(1, new OutputLayer.Builder(LossFunctions.LossFunction.MSE).weightInit(WeightInit.XAVIER).activation(Activation.IDENTITY).nOut(10).build()).setInputType(InputType.convolutional(5, 5, 3)).build();
            MultiLayerNetwork net = new MultiLayerNetwork(conf);
            net.init();
            int minibatch = 32;
            List<DataSet> list = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                list.add(new DataSet(Nd4j.rand(new int[] { minibatch, 3, 5, 5 }), Nd4j.rand(minibatch, 10)));
            }
            DataSetIterator iter = new ListDataSetIterator(list);
            INDArray expMean = Nd4j.valueArrayOf(new int[] { 1, 3 }, 0.5);
            // Expected variance of U(0,1) distribution: 1/12 * (1-0)^2 = 0.0833
            INDArray expVar = Nd4j.valueArrayOf(new int[] { 1, 3 }, 1 / 12.0);
            for (int i = 0; i < 10; i++) {
                iter.reset();
                net.fit(iter);
            }
            INDArray estMean = net.getLayer(0).getParam(BatchNormalizationParamInitializer.GLOBAL_MEAN);
            INDArray estVar;
            if (useLogStd) {
                INDArray log10std = net.getLayer(0).getParam(BatchNormalizationParamInitializer.GLOBAL_LOG_STD);
                estVar = Nd4j.valueArrayOf(log10std.shape(), 10.0).castTo(log10std.dataType());
                // stdev = 10^(log10(stdev))
                Transforms.pow(estVar, log10std, false);
                estVar.muli(estVar);
            } else {
                estVar = net.getLayer(0).getParam(BatchNormalizationParamInitializer.GLOBAL_VAR);
            }
            float[] fMeanExp = expMean.data().asFloat();
            float[] fMeanAct = estMean.data().asFloat();
            float[] fVarExp = expVar.data().asFloat();
            float[] fVarAct = estVar.data().asFloat();
            // System.out.println("Mean vs. estimated mean:");
            // System.out.println(Arrays.toString(fMeanExp));
            // System.out.println(Arrays.toString(fMeanAct));
            // 
            // System.out.println("Var vs. estimated var:");
            // System.out.println(Arrays.toString(fVarExp));
            // System.out.println(Arrays.toString(fVarAct));
            assertArrayEquals(fMeanExp, fMeanAct, 0.01f);
            assertArrayEquals(fVarExp, fVarAct, 0.01f);
        }
    }

    @Test
    @DisplayName("Check Mean Variance Estimate CNN Compare Modes")
    @Tag(TagNames.LONG_TEST)
    @Tag(TagNames.LARGE_RESOURCES)
    void checkMeanVarianceEstimateCNNCompareModes() throws Exception {
        Nd4j.getRandom().setSeed(12345);
        // Check that the internal global mean/variance estimate is approximately correct
        // First, Mnist data as 2d input (NOT taking into account convolution property)
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder().optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT).updater(Updater.RMSPROP).seed(12345).list().layer(0, new BatchNormalization.Builder().nIn(3).nOut(3).eps(1e-5).decay(0.95).useLogStd(false).build()).layer(1, new OutputLayer.Builder(LossFunctions.LossFunction.MSE).weightInit(WeightInit.XAVIER).activation(Activation.IDENTITY).nOut(10).build()).setInputType(InputType.convolutional(5, 5, 3)).build();
        MultiLayerNetwork net = new MultiLayerNetwork(conf);
        net.init();
        Nd4j.getRandom().setSeed(12345);
        MultiLayerConfiguration conf2 = new NeuralNetConfiguration.Builder().optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT).updater(Updater.RMSPROP).seed(12345).list().layer(0, new BatchNormalization.Builder().nIn(3).nOut(3).eps(1e-5).decay(0.95).useLogStd(true).build()).layer(1, new OutputLayer.Builder(LossFunctions.LossFunction.MSE).weightInit(WeightInit.XAVIER).activation(Activation.IDENTITY).nOut(10).build()).setInputType(InputType.convolutional(5, 5, 3)).build();
        MultiLayerNetwork net2 = new MultiLayerNetwork(conf2);
        net2.init();
        int minibatch = 32;
        for (int i = 0; i < 10; i++) {
            DataSet ds = new DataSet(Nd4j.rand(new int[] { minibatch, 3, 5, 5 }), Nd4j.rand(minibatch, 10));
            net.fit(ds);
            net2.fit(ds);
            INDArray globalVar = net.getParam("0_" + BatchNormalizationParamInitializer.GLOBAL_VAR);
            INDArray log10std = net2.getParam("0_" + BatchNormalizationParamInitializer.GLOBAL_LOG_STD);
            INDArray globalVar2 = Nd4j.valueArrayOf(log10std.shape(), 10.0).castTo(log10std.dataType());
            // stdev = 10^(log10(stdev))
            Transforms.pow(globalVar2, log10std, false);
            globalVar2.muli(globalVar2);
            assertEquals(globalVar, globalVar2);
        }
    }

    @Test
    @DisplayName("Test Batch Norm")
    void testBatchNorm() throws Exception {
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder().seed(12345).updater(new Adam(1e-3)).activation(Activation.TANH).list().layer(new ConvolutionLayer.Builder().nOut(5).kernelSize(2, 2).build()).layer(new BatchNormalization()).layer(new ConvolutionLayer.Builder().nOut(5).kernelSize(2, 2).build()).layer(new OutputLayer.Builder().activation(Activation.SOFTMAX).lossFunction(LossFunctions.LossFunction.MCXENT).nOut(10).build()).setInputType(InputType.convolutionalFlat(28, 28, 1)).build();
        MultiLayerNetwork net = new MultiLayerNetwork(conf);
        net.init();
        DataSetIterator iter = new EarlyTerminationDataSetIterator(new MnistDataSetIterator(32, true, 12345), 10);
        net.fit(iter);
        MultiLayerNetwork net2 = new TransferLearning.Builder(net).fineTuneConfiguration(FineTuneConfiguration.builder().updater(new AdaDelta()).build()).removeOutputLayer().addLayer(new BatchNormalization.Builder().nOut(3380).build()).addLayer(new OutputLayer.Builder().activation(Activation.SOFTMAX).lossFunction(LossFunctions.LossFunction.MCXENT).nIn(3380).nOut(10).build()).build();
        net2.fit(iter);
    }

    @Test
    @DisplayName("Test Batch Norm Recurrent Cnn 1 d")
    void testBatchNormRecurrentCnn1d() {
        // Simple sanity check on CNN1D and RNN layers
        for (boolean rnn : new boolean[] { true, false }) {
            MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder().seed(12345).weightInit(WeightInit.XAVIER).convolutionMode(ConvolutionMode.Same).list().layer(rnn ? new LSTM.Builder().nOut(3).build() : new Convolution1DLayer.Builder().kernelSize(3).stride(1).nOut(3).build()).layer(new BatchNormalization()).layer(new RnnOutputLayer.Builder().nOut(3).activation(Activation.TANH).lossFunction(LossFunctions.LossFunction.MSE).build()).setInputType(InputType.recurrent(3)).build();
            MultiLayerNetwork net = new MultiLayerNetwork(conf);
            net.init();
            INDArray in = Nd4j.rand(new int[] { 1, 3, 5 });
            INDArray label = Nd4j.rand(new int[] { 1, 3, 5 });
            INDArray out = net.output(in);
            assertArrayEquals(new long[] { 1, 3, 5 }, out.shape());
            net.fit(in, label);
            log.info("OK: {}", (rnn ? "rnn" : "cnn1d"));
        }
    }

    @Test
    @DisplayName("Test Input Validation")
    void testInputValidation() {
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder().list().layer(new BatchNormalization.Builder().nIn(10).nOut(10).build()).build();
        MultiLayerNetwork net = new MultiLayerNetwork(conf);
        net.init();
        INDArray in1 = Nd4j.create(1, 10);
        INDArray in2 = Nd4j.create(1, 5);
        INDArray out1 = net.output(in1);
        try {
            INDArray out2 = net.output(in2);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("expected input"));
        }
    }
}
