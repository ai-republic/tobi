/**
 * Copyright © 2013 Antonin Stefanutti (antonin.stefanutti@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.astefanutti.metrics.cdi.se;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Gauge;

@ApplicationScoped
public class GaugeInjectionBean {

    private long gauge;

    @Gauge(name = "gaugeInjection", unit=MetricUnits.NONE)
    public long getGauge() {
        return gauge;
    }

    public void setGauge(long gauge) {
        this.gauge = gauge;
    }
}