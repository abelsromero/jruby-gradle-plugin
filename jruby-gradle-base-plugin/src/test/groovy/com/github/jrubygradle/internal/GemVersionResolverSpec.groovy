package com.github.jrubygradle.internal

import spock.lang.*

import org.gradle.api.artifacts.DependencyResolveDetails
import org.gradle.api.artifacts.ModuleVersionSelector

class TestGemVersionResolver extends GemVersionResolver {
    void firstRun() {
        this.versions = [:]
    }
    void log(Closure msg) {
    }
}

class GemVersionResolverSpec extends Specification {

    def "ignore non rubygems group modules"() {
        setup:
            GemVersionResolver subject = new TestGemVersionResolver()
            def details = Mock(DependencyResolveDetails)
            def requested = Mock(ModuleVersionSelector)
            details.getRequested() >> requested

        when:
            subject.resolve( details )

        then:
            0 * details.useVersion(_)
            0 * details.setTarget(_)
    }

    def "keep rubygems group modules as is on first visit"() {
        setup:
            GemVersionResolver subject = new TestGemVersionResolver()
            def details = Mock(DependencyResolveDetails)
            def requested = Mock(ModuleVersionSelector)
            details.getRequested() >> requested

        when:
            subject.resolve( details )

        then:
            1 * requested.getGroup() >> 'rubygems'
            1 * requested.getVersion() >> '1.2.3'
            0 * details.useVersion(_)
            0 * details.setTarget(_)
            subject.toString() != new GemVersionResolver().toString()
    }

    def "narrows down version range on second visit"() {
        setup:
            GemVersionResolver subject = new TestGemVersionResolver()
            def details1 = Mock(DependencyResolveDetails)
            def requested1 = Mock(ModuleVersionSelector)
            details1.getRequested() >> requested1

            def details2 = Mock(DependencyResolveDetails)
            def requested2 = Mock(ModuleVersionSelector)
            details2.getRequested() >> requested2

        when:
            subject.resolve( details1 )
            subject.resolve( details2 )

        then:
            1 * requested1.getGroup() >> 'rubygems'
            1 * requested1.getVersion() >> '[1.2.3,3.4.5]'
            1 * requested2.getGroup() >> 'rubygems'
            1 * requested2.getVersion() >> '[2.3.4,4.5.6]'
            0 * details1.useVersion(_)
            0 * details1.setTarget(_)
            1 * details2.useVersion('[2.3.4,3.4.5]')
            0 * details2.setTarget(_)
    }

    def "version conflict"() {
        setup:
            GemVersionResolver subject = new TestGemVersionResolver()
            def details1 = Mock(DependencyResolveDetails)
            def requested1 = Mock(ModuleVersionSelector)
            details1.getRequested() >> requested1

            def details2 = Mock(DependencyResolveDetails)
            def requested2 = Mock(ModuleVersionSelector)
            details2.getRequested() >> requested2

        when:
            subject.resolve( details1 )
            subject.resolve( details2 )

        then:
            1 * requested1.getGroup() >> 'rubygems'
            1 * requested1.getVersion() >> '[1.2.3,2.3.4]'
            1 * requested2.getGroup() >> 'rubygems'
            2 * requested2.getVersion() >> '[3.4.5,4.5.6]'
            def exception = thrown(RuntimeException)
            exception.message == 'there is no overlap for [1.2.3,2.3.4] and [3.4.5,4.5.6]'
    }
}
