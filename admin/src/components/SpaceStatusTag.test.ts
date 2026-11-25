import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import SpaceStatusTag from './SpaceStatusTag.vue'

function mountTag(status: number) {
  return mount(SpaceStatusTag, {
    props: { space: { status } },
    global: { plugins: [ElementPlus] },
  })
}

describe('SpaceStatusTag', () => {
  it('should_render_free_label', () => {
    expect(mountTag(0).text()).toBe('空闲')
  })

  it('should_render_occupied_label', () => {
    expect(mountTag(1).text()).toBe('占用')
  })

  it('should_render_reserved_label', () => {
    expect(mountTag(2).text()).toBe('预约锁定')
  })

  it('should_render_maintenance_label', () => {
    expect(mountTag(3).text()).toBe('维护')
  })
})
