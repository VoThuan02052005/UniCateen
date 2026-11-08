import React from 'react';
import { api } from '../../api/client.js';
import { dateTime } from '../../format.js';
import { ErrorBox, Page, Table, useLoad } from '../../components/ui.jsx';

/** Nhat ky kiem toan chi doc (NFR09). */
export default function AuditLogPage() {
  const [rows, , error] = useLoad(() => api.get('/api/accounts/audit-logs'));
  return (
    <Page title="Nhật ký kiểm toán">
      <ErrorBox error={error} />
      <Table rows={rows} columns={[
        { title: 'Thời điểm', render: (a) => dateTime(a.createdAt) },
        { key: 'actor', title: 'Người thực hiện' },
        { key: 'action', title: 'Thao tác' },
        { title: 'Đối tượng', render: (a) => `${a.entityType} #${a.entityId}` },
        { title: 'Trước', render: (a) => <code>{a.beforeValue}</code> },
        { title: 'Sau', render: (a) => <code>{a.afterValue}</code> },
      ]} />
    </Page>
  );
}
